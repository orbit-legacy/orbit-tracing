import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.concurrent.Task;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.contrib.orbit.SpanContextPropagation;
import io.opentracing.util.GlobalTracer;

public class TestActorLifetimeSpan extends OrbitEnvBuilder {

	public TestActorLifetimeSpan() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void testProcessMessagesConcurrentlyByOneActor() {
		
        Span spanParent = GlobalTracer.get().buildSpan("processMessagesConcurrentlyByOneActor").start();
        
        System.out.println("------------DEMO processing messages concurrently by multiple actor instances-----------------");
        // Send messages concurrently
        // The messages are sent in sequence but it is processed by actors concurrently, so the responses are received without orders.   
        final int total = 10;
        for( int i = 0; i < total; i++) {
            Span span = GlobalTracer.get().buildSpan("action " + i).asChildOf(spanParent).start();	            
            
            try ( Scope scopeItem = activeScope(span)) {    
            	
	        	String message = "Welcome to orbit " + i;
	            System.out.println("Message to send: " + message);
	            
	            // Each message is processed by a new instance of the HelloActor
	            try ( SpanContextPropagation scp = new SpanContextPropagation(span)) {

		        	TracedOrbitTester actor = Actor.getReference(TracedOrbitTester.class, String.format("%d", 0));
		        	Task<String> task = actor.sayHello(message);
		        	
		        	if ( (i+1) == total) {
		        		task.join();
		        	}
		        	assertTrue( actor.getIdentity().length() > 0);
	            }
            } finally {
            	span.finish();
            }
        }
        
        spanParent.finish();        
	}

	@Test
	public void processMessagesSequenciallyByOneActor() {
		
        System.out.println("------------DEMO processing messages in sequence by one actor -----------------");
        final int total = 10;
        
        Span spanParent = GlobalTracer.get().buildSpan("processMessagesSequencialByOneActor")
				  .withTag("author", "zjj")
				  .withTag("app", "hello concurrency")
				  .start();
        
        spanParent.log("Loop starting");
        
        for( int i = 0; i < total; i++) {
        	
            Span span = GlobalTracer.get()
            		.buildSpan("action " + i)
            		.asChildOf(spanParent)
            		.start();
            span.setTag("Kind", "Code block in Loop");
            
            try ( Scope scopeItem = activeScope(span)) {      
            	
	        	String message = "Welcome to orbit " + i;
	            System.out.println("Message to send: " + message);
	            
	        	// Each message is processed by the actor 0, therefore all the messages are sent in sequence and processed in sequence too. The order of processing is opposite to the order of sending.	        		        	
	            try ( SpanContextPropagation scp = new SpanContextPropagation(span)) {

	                Task<String> task = Actor.getReference(TracedOrbitTester.class, "0").sayHello(message);                    
		        	task.join();
	            }
            } 
        	span.finish();
        } // for ...
        
        assertTrue(spanParent != null);
    	spanParent.finish();
	}

	@Test
	public void processMessagesSequenciallyByMultipleActor() {
		
        Span spanParent = GlobalTracer.get().buildSpan("processMessagesSequenciallyByMultipleActor").start();
		
        System.out.println("------------DEMO processing messages in sequence by multiple actor intances -----------------");
        final int total = 10;
        for( int i = 0; i < total; i++) {
            Span span = GlobalTracer.get().buildSpan("action " + i).asChildOf(spanParent).start();	            
            try ( Scope scopeItem = activeScope(span)) {    
            	
	        	String message = "Welcome to orbit " + i;
	            System.out.println("Message to send: " + message);
	            
	        	// Each message is processed by a new instance of the HelloActor but in the order of receiving
	            try ( SpanContextPropagation scp = new SpanContextPropagation(span)) {
		            Actor.getReference(TracedOrbitTester.class, String.format("%d", i)).sayHello(message).join();
	            }
            } finally {
            	span.finish();
            }
        }
    
        assertTrue( spanParent != null);
        spanParent.finish();
	}
}
