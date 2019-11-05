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

import cloud.orbit.actors.extensions.InvocationHandlerExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* A Extension to collect the opentracing data during the invoke time
*
* Created by Jianjun Zhou on 10/08/2019.
*/
public class ActorInvocationHandlerExtension implements InvocationHandlerExtension
{
	private AtomicBoolean acceptCalls = new AtomicBoolean(true);
	
	private ActorInvocationHandlerSpan invocationSpan = new ActorInvocationHandlerSpan();
	
	@Override
	public Task<Void> beforeInvoke(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, Map<?, ?> invocationHeaders)
	{
		if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");
		
		if ( targetObject != null && (targetObject instanceof AbstractActor)) {
			invocationSpan.createSpan((AbstractActor<?>)targetObject, targetMethod.getName(), startTimeNanos);	
	    }		
		
		return Task.done();
	}

	@Override
	public Task<Void> afterInvoke(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
	{
		if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");
   	
		if ( targetObject != null && (targetObject instanceof AbstractActor)) {   		
			invocationSpan.endSpan((AbstractActor<?>)targetObject, targetMethod.getName(), startTimeNanos);
	    }
       
		return Task.done();
	}

	@Override
	public Task<Void> afterInvokeChain(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
	{
		return Task.done();
	}

}
