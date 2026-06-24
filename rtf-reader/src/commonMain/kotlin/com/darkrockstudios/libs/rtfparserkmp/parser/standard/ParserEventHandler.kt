/*
 * Copyright 2013 Jon Iles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.darkrockstudios.libs.rtfparserkmp.parser.standard

import com.darkrockstudios.libs.rtfparserkmp.parser.RtfEvent
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfListener
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command

/**
 * Pass this event to the listener.
 */
internal fun RtfEvent.fireTo(listener: RtfListener) {
    when (this) {
        RtfEvent.DocumentStart -> listener.processDocumentStart()
        RtfEvent.DocumentEnd -> listener.processDocumentEnd()
        RtfEvent.GroupStart -> listener.processGroupStart()
        RtfEvent.GroupEnd -> listener.processGroupEnd()
        is RtfEvent.Text -> listener.processString(text)
        is RtfEvent.BinaryBytes -> listener.processBinaryBytes(bytes)
        is RtfEvent.Command -> listener.processCommand(command, parameter, hasParameter, optional)
    }
}

/**
 * Represents a handler which will consume events raised by the parser and handle them
 * appropriately. By default this will typically mean passing them to the listener,
 * but there may be cases where we may wish to implement something like a state machine
 * to consume a set of related events, then take some action based on the complete set
 * of events read, rather than reacting to events one at a time.
 *
 * This interface allows this functionality to be switched in and out as required.
 */
internal interface ParserEventHandler {
    /**
     * The parser informs the handler of an event.
     */
    fun handleEvent(event: RtfEvent)

    /**
     * Retrieve the last event seen by the handler.
     */
    val lastEvent: RtfEvent?

    /**
     * Assumes the handler is buffering events, and removes the last event from this buffer.
     */
    fun removeLastEvent()

    /**
     * Returns false if this handler is OK to receive further events, or true
     * if this handler is complete, and the previous handler should be used again.
     * This assumes that the parser is keeping a stack of handlers and popping the
     * last handler from the stack when the current handler has consumed all the events
     * it can.
     */
    val isComplete: Boolean
}

/**
 * Default parser event handler. Passes events to the listener. In this implementation
 * the events are queued to allow later events to modify earlier events before they are
 * passed to the listener. For example, we coalesce consecutive string events together.
 */
internal class DefaultEventHandler(private val listener: RtfListener) : ParserEventHandler {
    private val events = ArrayDeque<RtfEvent>()

    /**
     * If we've reached the end of the document, flush all queued events to
     * the listener and pass on the document end event.
     * If we have received consecutive string events, coalesce them into
     * a single event in the buffer.
     * If the buffer has reached its maximum size, remove the event from the
     * front of the buffer and pass this to the listener.
     */
    override fun handleEvent(event: RtfEvent) {
        if (event == RtfEvent.DocumentEnd) {
            flushEvents()
            event.fireTo(listener)
        } else {
            var newEvent = event
            val lastEvent = events.lastOrNull()
            if (lastEvent is RtfEvent.Text && event is RtfEvent.Text) {
                newEvent = mergeStringEvents(event)
            }

            events.add(newEvent)

            if (events.size > MAX_EVENTS) {
                events.removeFirst().fireTo(listener)
            }
        }
    }

    /**
     * It's always valid for this handler to continue processing events,
     * so we always return false.
     */
    override val isComplete: Boolean
        get() = false

    /**
     * Allows the caller to see the event at the end of the buffer.
     */
    override val lastEvent: RtfEvent?
        get() = events.lastOrNull()

    /**
     * Allows the caller to remove the last event from the buffer.
     */
    override fun removeLastEvent() {
        events.removeLast()
    }

    /**
     * Removes the string event from the end of the buffer, merges it with the string
     * event we've just received, and adds the new event to the end of the buffer.
     */
    private fun mergeStringEvents(event: RtfEvent.Text): RtfEvent {
        val lastEvent = events.removeLast() as RtfEvent.Text
        return RtfEvent.Text(lastEvent.text + event.text)
    }

    /**
     * Passes any remaining events in the buffer to the listener and clears the event buffer,
     */
    private fun flushEvents() {
        for (event in events) {
            event.fireTo(listener)
        }
        events.clear()
    }

    private companion object {
        const val MAX_EVENTS = 5
    }
}

/**
 * The upr command is used to wrap two different versions of the same set of
 * formatting commands. The first set of formatting commands uses ANSI encoding,
 * the second set uses Unicode. The upr command is expected to appear
 * in its own group, so this handler can be used to consume all of the RTF events
 * received up to the end of the group It can then pass the Unicode version of
 * the command it wraps to the listener, discarding the ANSI version.
 */
internal class UprHandler(private val handler: ParserEventHandler) : ParserEventHandler {
    private var groupCount = 1
    private var complete = false
    private val events = ArrayList<RtfEvent>()

    /**
     * Buffers events until the end of the group containing the upr command is reached.
     * Once the end of the group is reached, the buffered events representing the
     * Unicode content is sent to the listener.
     */
    override fun handleEvent(event: RtfEvent) {
        events.add(event)
        when (event) {
            RtfEvent.GroupStart -> ++groupCount
            RtfEvent.GroupEnd -> --groupCount
            else -> {
            }
        }

        if (groupCount == 0) {
            processCommands()
        }
    }

    /**
     * Retrieve the last event seen by the handler.
     */
    override val lastEvent: RtfEvent?
        get() = events[events.size - 1]

    /**
     * Assumes the handler is buffering events, and removes the last event from this buffer.
     */
    override fun removeLastEvent() {
        events.removeAt(events.size - 1)
    }

    /**
     * Returns true once the end of the group containing the upr command as been reached.
     */
    override val isComplete: Boolean
        get() = complete

    /**
     * Extracts the Unicode version of the commands wrapped by the upr
     * command and passes them to the listener.
     */
    private fun processCommands() {
        var index = 0
        while (true) {
            if (index == events.size) {
                throw IllegalStateException("UPR command: structure not recognised")
            }
            val event = events[index]
            if (event is RtfEvent.Command && event.command == Command.ud) {
                break
            }
            ++index
        }

        if (index == events.size) {
            throw IllegalStateException("UPR command: structure not recognised: unable to locate UD command")
        }

        ++index
        if (events[index] != RtfEvent.GroupStart) {
            throw IllegalStateException("UPR command: expecting group start, found: ${events[index]}")
        }

        ++index
        var endIndex = index
        var localGroupCount = 1
        while (true) {
            if (endIndex == events.size) {
                break
            }

            when (events[endIndex]) {
                RtfEvent.GroupStart -> ++localGroupCount
                RtfEvent.GroupEnd -> --localGroupCount
                else -> {
                }
            }

            if (localGroupCount == 0) {
                break
            }
            ++endIndex
        }

        if (index == events.size) {
            throw IllegalStateException("UPR command: structure not recognised: unable to locate UD group end")
        }

        while (index <= endIndex) {
            handler.handleEvent(events[index])
            ++index
        }

        complete = true
    }
}
