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

import cloud.orbit.actors.runtime.ActorTaskContext;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

public class SpanContextPropagation implements AutoCloseable {

	final private ActorTaskContext atc;
	
	public SpanContextPropagation(Span span) {
		atc = pushSpanContext( span);
	}

	@Override
	public void close() {
		try {
			popSpanContext(atc);
		} catch (Exception ex){			
		}
	}

	// Push the span context to the next actor action
	static private ActorTaskContext pushSpanContext(Span span) {
		if ( span == null) return null;		
		
		try {
			// Store the span name
			span.setBaggageItem(TracedStage.SPAN_OPERATION_NAME, span.toString());
			
	        SpanTextMap spanContext = new SpanTextMap();
	        GlobalTracer.get().inject(span.context(), Format.Builtin.TEXT_MAP, spanContext);	        	
	    	ActorTaskContext atc = ActorTaskContext.pushNew();
	    	atc.setProperty( TracedStage.SPAN_CONTEXT, spanContext);
	    	
	    	return atc;
		} catch (Exception ex) {
		}
		
		return null;
	}
	
	// Pop out the span context after the actor action is done
	static private void popSpanContext( ActorTaskContext atc) {
		if ( atc == null) return;
		atc.pop();
	}
	
	// Generate the span context from the header/property of the TaskContext
	static public SpanContext getSpanContext() {
		
		SpanContext spanContext = null; 
		
		ActorTaskContext cp = ActorTaskContext.current();
		if ( cp != null) {
			Object data = cp.getProperty( TracedStage.SPAN_CONTEXT);			
			if  ( data != null ) {
				SpanTextMap spanContextData = (SpanTextMap)data;
				spanContext = GlobalTracer.get().extract(Format.Builtin.TEXT_MAP, spanContextData);		        
			}
		}
		return spanContext;
	}	
}
