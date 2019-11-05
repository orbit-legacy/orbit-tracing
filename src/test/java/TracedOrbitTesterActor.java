/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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



import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

import java.util.concurrent.TimeUnit;

/**
 * Created by joe@bioware.com on 2016-04-26.
 */
public class TracedOrbitTesterActor extends AbstractActor<Object> implements TracedOrbitTester
{
    public TracedOrbitTesterActor() {
    }

    public Task<String> sleep() {
    	try {
    		// Sleep X ms and then send the message back
            long delay = (long)(10*1000*1000);
    		TimeUnit.MILLISECONDS.sleep( delay);
        	System.out.println("Actor sleeping " + delay + " ms");
    	} catch( Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " Cause: " + ex.getCause());
    	}

        return Task.fromValue("Actor sleep");
    }

	public Task<String> sayHello(String greeting)
    {
    	Span span = GlobalTracer.get().buildSpan(getSpanNameForMethod("sayHello")).start();
    	try {
    		// Sleep X ms and then send the message back
            long delay = (long)(50);
    		TimeUnit.MILLISECONDS.sleep( delay);
    	} catch( Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " Cause: " + ex.getCause());
    	} finally {
    		span.finish();
    	}

    	System.out.println("Actor: " + greeting);

        return Task.fromValue("You said: '" + greeting
                + "', I say: Hello from " + System.identityHashCode(this) + " !");
    }

	public Task<String> saySomethingNice(String greeting)
    {
		{
			Span span = GlobalTracer.get().buildSpan("sayHello#1").start();
			try {
				sayHello("Good morning").join();
			} finally {
				span.finish();
			}
		}
		Span span = GlobalTracer.get().buildSpan("sayHello#2").start();
		try {
			return sayHello( greeting);
		} finally {
			span.finish();
		}
    }

	public Task<String> sayHelloWithLongTimeToProcess(String greeting) {
    	try {
    		// Sleep X ms and then send the message back
            long delay = (long)(Math.random()*1000*100);
    		TimeUnit.MILLISECONDS.sleep( delay);
    	} catch( Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " Cause: " + ex.getCause());
    	}


        return Task.fromValue("You said: '" + greeting
                + "', I say: Hello from " + System.identityHashCode(this) + " !");
    }


	private String getSpanNameForMethod(String methodName) {

		String name = this.getClass().getSimpleName() + "::" + this.actorIdentity() + "::" + methodName;
		if ( GlobalTracer.get().activeSpan() != null) {
			name += "::parent." + GlobalTracer.get().activeSpan().context().baggageItems().toString();
		}

		return name;
	}
}
