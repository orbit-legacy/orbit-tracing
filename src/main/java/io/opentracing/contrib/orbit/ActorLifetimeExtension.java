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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.util.GlobalTracer;

/**
* A Extension to collect the opentracing data during the actor lifetime
*
* Created by Jianjun Zhou on 10/08/2019.
*/
public class ActorLifetimeExtension implements LifetimeExtension
{	
	private AtomicBoolean acceptCalls = new AtomicBoolean(true);
	
	private SpanContext rootSpanContext = null;
	private static String PRE_ACTIVATION_TIME = "preActivationTimestampMs";
	private static String PRE_DEACTIVATION_TIME = "preDeActivationTimestampMs";
     
	public ActorLifetimeExtension()
	{
		buildRootSpan();
	}

	// Build the root span for all actors. It only be called at the construction time.
	private void buildRootSpan() {
		if ( rootSpanContext != null) {
			return;
		}
		Span rootSpan = GlobalTracer.get().buildSpan("Lifetime.Actors").ignoreActiveSpan().start();
		rootSpan.log("Created the root span for all actors");
		TracedActorThread.setTagOnCurrentThread(rootSpan);
		rootSpanContext = rootSpan.context();
		rootSpan.finish();
	}

	// Build the actor span
	private Span buildActorSpan( final AbstractActor<?> actor) {
		// Get the actor name
		String spanName = ActorLifetimeSpan.getActorName( actor);
		
		// Create the actor span
		Span actorSpan = GlobalTracer.get().buildSpan(spanName)
				.ignoreActiveSpan()
				.addReference(References.FOLLOWS_FROM, rootSpanContext)
				.start();		
		// Store the actor span
		ActorLifetimeSpan.getInstance().putActorSpan(actor, actorSpan);
		// Keep the time for future reference to calculate the full time length during the activation 
		actorSpan.setBaggageItem(PRE_ACTIVATION_TIME, String.valueOf(System.currentTimeMillis()));
		TracedActorThread.setTagOnCurrentThread(actorSpan);		
		TracedActorThread.setTagOnActorObject(actorSpan, actor);
		
		return actorSpan;
	}

	// Build a life time event span : activation and deactivation
	private void buildLifetimeEventSpan(final AbstractActor<?> actor, final String spanName, final long spanStartTimestampMs) {
		
		Span actorSpan = ActorLifetimeSpan.getInstance().getActorSpan(actor);
		if ( actorSpan == null)
			return;
		
		// 1. Create the activation span for the actor lifetime
		Span actorActivationSpan = GlobalTracer.get().buildSpan( spanName)
				.ignoreActiveSpan()
				.withStartTimestamp(TimeUnit.MILLISECONDS.toMicros( spanStartTimestampMs))
				.addReference(References.FOLLOWS_FROM, actorSpan.context())
				.start();
		TracedActorThread.setTagOnCurrentThread(actorActivationSpan);

		// Finish immediately as we take this event as a span action
		actorActivationSpan.finish();		
		
		// 2. Create the span for the caller
		// It seems the SpanContext is never passed to an lifetime event so the following span is never created either. 
		// #TODO Find the best way to create the caller span
		/*
		SpanContext scCaller = SpanContextPropagation.getSpanContext();			
		if ( scCaller != null) {
			Span callerSpan = GlobalTracer.get().buildSpan( spanName)
					.ignoreActiveSpan()
					.withStartTimestamp(TimeUnit.MILLISECONDS.toMicros( spanStartTimestampMs))
					.addReference(References.FOLLOWS_FROM, scCaller)
					.start();
			TracedActorThread.setTagOnCurrentThread(callerSpan);
	
			// Finish immediately as we take this event as a span action
			callerSpan.finish();
		}
		*/
	}
	
	// Build an actor span
	public Task<?> preActivation(final AbstractActor<?> actor)
	{     
		if(!acceptCalls.get()) return Task.done();
		
		// Create the Actor span
		Span actorSpan = null; 
		
		try {

			actorSpan = buildActorSpan(actor);
			return Task.done();
			
		} finally {
			// Close the Actor span
			actorSpan.finish();			
		}
	}

	public Task<?> postActivation(final AbstractActor<?> actor)
	{
		if(!acceptCalls.get()) return Task.done();
		
		// Build the lifetime event span for the whole activation period			
		Span actorSpan = ActorLifetimeSpan.getInstance().getActorSpan(actor);
		if ( actorSpan != null) {
			long startTimestamp = Long.valueOf(actorSpan.getBaggageItem(PRE_ACTIVATION_TIME));
			buildLifetimeEventSpan(actor, "Activation", startTimestamp);
		}
		
		return Task.done();
	}

	public Task<?> preDeactivation(final AbstractActor<?> actor)
	{
		if(!acceptCalls.get()) return Task.done();
		
		// Keep the de-activation start time, which will be refered as the postDeactivation time		
		Span actorSpan = ActorLifetimeSpan.getInstance().getActorSpan(actor);
		if ( actorSpan != null) {
			actorSpan.setBaggageItem(PRE_DEACTIVATION_TIME, String.valueOf(System.currentTimeMillis()));
		}

		return Task.done();
	}

	public Task<?> postDeactivation(final AbstractActor<?> actor)
	{
		if(!acceptCalls.get()) return Task.done();
		
		// Build the lifetime event span for the whole activation period			
		Span actorSpan = ActorLifetimeSpan.getInstance().getActorSpan(actor);
		if ( actorSpan != null) {
			long startTimestamp = Long.valueOf(actorSpan.getBaggageItem(PRE_DEACTIVATION_TIME));
			buildLifetimeEventSpan(actor, "DeActivation", startTimestamp);
		}
		
		// Remove the actor span from the global data structure 
		ActorLifetimeSpan.getInstance().removeActorSpan(actor);

		return Task.done();
	}
}