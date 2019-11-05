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

import java.util.ArrayList;
import cloud.orbit.actors.Stage;

/**
* A tracing class to enable open tracing for the active Orbit stage
*
* Created by Jianjun Zhou on 10/08/2019.
*/
public class TracedStage {

	static public final String SPAN_CONTEXT = "SpanContext";
	static public final String SPAN_OPERATION_NAME = "SpanParentName";
	static public final String ACTOR_LIFETIME_SPAN_CONTEXT = "ActorLifeTimeSpanContext";
	
	// Enable tracing for an Orbit stage
	static public boolean enableTracing(Stage stage) {
		
		if ( stage == null) return false;
		
        // Add the open tracing extension 
        stage.addExtension(new ActorLifetimeExtension());
        stage.addExtension(new ActorInvocationHandlerExtension()); 
        
        // Add the sticky headers, which will be used to propagate tracing context cross boundaries in Orbit   
        ArrayList<String> headers = new ArrayList<String>();
        headers.add( TracedStage.SPAN_CONTEXT);
        headers.add( TracedStage.SPAN_OPERATION_NAME);        
        headers.add( TracedStage.ACTOR_LIFETIME_SPAN_CONTEXT);        
        stage.addStickyHeaders(headers);        
        
        return true;

	}	
}
