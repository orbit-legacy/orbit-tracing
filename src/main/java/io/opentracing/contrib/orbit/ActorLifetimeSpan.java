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

import java.util.concurrent.ConcurrentHashMap;

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.RemoteReference;
import io.opentracing.Span;

public class ActorLifetimeSpan {
	
	final static private ActorLifetimeSpan instance = new ActorLifetimeSpan();
	private ConcurrentHashMap<String, Span> actorSpans = new ConcurrentHashMap<String, Span>();
	
	private ActorLifetimeSpan() {
		// TODO Auto-generated constructor stub
	}

	public static ActorLifetimeSpan getInstance() {		
		return instance; 
	}
	
	public void putActorSpan(final AbstractActor<?> actor, final Span span) {
		actorSpans.put( getActorName(actor), span);
	}
	
	public Span getActorSpan(final AbstractActor<?> actor) {
		return actorSpans.get( getActorName(actor));
	}

	public Span removeActorSpan(final AbstractActor<?> actor) {
		return actorSpans.remove(getActorName(actor));
	}

	public static String getActorName( final AbstractActor<?> actor) {
		Object id = RemoteReference.getId(actor);
		String name = "Lifetime." + RemoteReference.getInterfaceClass(actor).getSimpleName();
		if ( id != null && id.toString().compareTo("null") != 0)  {
			name += "." + id.toString();
		}
		return name;
	}   
	
}
