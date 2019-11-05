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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorTaskContext;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

/**
* A span designed to invoke method for Orbit invoke handler extensions
*
* Created by Jianjun Zhou on 10/08/2019.
*/
public class ActorInvocationHandlerSpan {

	private static final Logger logger = LoggerFactory.getLogger(ActorInvocationHandlerSpan.class);
	
	private ConcurrentHashMap<String, Span> invokeSpans = new ConcurrentHashMap<String, Span>();
	private ConcurrentHashMap<String, Scope> invokeScopes = new ConcurrentHashMap<String, Scope>();
	private ConcurrentHashMap<String, Span> actorCallerSpans = new ConcurrentHashMap<String, Span>();

	public ActorInvocationHandlerSpan() {
	}

	private Span buildSpan( final String spanName, SpanContext sc) {
		
		SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(spanName).ignoreActiveSpan();		
		
		if ( sc != null) {
			spanBuilder.asChildOf( sc);
		}
		
		Span span = spanBuilder.start();    		
		span.setTag("Kind", "Orbit Actor");
		TracedActorThread.setTagOnCurrentThread(span);
		
		return span;
	}
	
	//  Create and active the Span
	public void createSpan(final AbstractActor<?> actor, final String methodName, final long startTimeNanos) {
			    		
		// We will build two spans. One for the caller span, One for the Actor lifetime span.
		
		// 1. Caller span
		String spanName = buildSpanName(actor, methodName);
		String spanKey = buildSpanKey(spanName, startTimeNanos);
		
		SpanContext scCaller = SpanContextPropagation.getSpanContext();
		if ( scCaller != null) {
			Span callerSpan = this.buildSpan( spanName, scCaller);
			TracedActorThread.setTagOnActorObject(callerSpan, actor);
			
			logger.debug("Span started: " + callerSpan.toString() + " start at " + startTimeNanos + " from Parent: " + ((scCaller != null)? scCaller.baggageItems().toString():" none"));
			
			invokeSpans.put( spanKey, callerSpan);
			
			Scope scope = GlobalTracer.get().scopeManager().activate(callerSpan, false);		
			invokeScopes.put( spanKey, scope);
		}

		// 2. Actor lifetime span
		Span actorLifeTimeSpan = ActorLifetimeSpan.getInstance().getActorSpan(actor);
		if ( actorLifeTimeSpan != null) {
			Span actorSpan = this.buildSpan( spanName, actorLifeTimeSpan.context());
			actorCallerSpans.put( spanKey, actorSpan);
		}
	}	
   
	// End the current active span
	public void endSpan(final AbstractActor<?> actor, final String methodName, final long startTimeNanos) {
		
		String spanKey = buildSpanKey(buildSpanName(actor, methodName), startTimeNanos);

		// 1. Finish the caller span
		Scope scope = invokeScopes.get( spanKey);
		if ( scope != null) {
			scope.close();
			invokeScopes.remove( spanKey);
		}
		
		Span span = invokeSpans.get( spanKey);
		if ( span != null) {
			span.finish();
			logger.debug("Span finished: " + span.toString() + " start at " + startTimeNanos + " end at " + System.nanoTime() + " from Parent: " + ((span.context() != null)? span.context().baggageItems().toString():" none"));
			
			invokeSpans.remove( spanKey);
		}
		
		// 2. Finish the Actor lifetime span
		span = actorCallerSpans.get( spanKey);
		if ( span != null) {
			span.finish();
			actorCallerSpans.remove( spanKey);
		}
	}
	
	private String buildSpanName(final AbstractActor<?> actor, final String methodName) {
		return ActorLifetimeSpan.getActorName(actor) + "." + methodName;
	}
	
	private String buildSpanKey(final String spanName, final long startTimeNanos) {
		return spanName + startTimeNanos;
	}
}
