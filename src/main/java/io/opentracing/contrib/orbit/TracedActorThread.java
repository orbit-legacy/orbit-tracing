/*
Copyright (C) 2019 Electronic Arts Inc.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1.  Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
2.  Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
    its contributors may be used to endorse or promote products derived
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package io.opentracing.contrib.orbit;

import cloud.orbit.actors.runtime.AbstractActor;
import io.opentracing.Span;

public class TracedActorThread {

	static private final String THREAD_ID = "ThreadId";
	static private final String THREAD_NAME = "ThreadName";
	static private final String ACTOR_OBJECT_ID = "ActorObjectId";
	public TracedActorThread() {
	}

	// Set a span tag for the current thread information 
	static public void setTagOnCurrentThread(Span span) {
		if ( span != null) {
			span.setTag(THREAD_ID, Thread.currentThread().getId());			
			span.setTag(THREAD_NAME, Thread.currentThread().getName());			
		}
	}
	
	static public void setTagOnActorObject(Span span, final AbstractActor<?> actor) {
		if ( span != null) {
			span.setTag(ACTOR_OBJECT_ID, System.identityHashCode(actor));			
		}
	}	
}
