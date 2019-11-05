import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.Stage.Builder;
import cloud.orbit.concurrent.Task;
import io.opentracing.*;
import io.opentracing.contrib.orbit.TracedStage;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalScopeManager;

public class OrbitEnvBuilder {

	static private Stage currentStage = null;
	
	static protected MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager());

	public OrbitEnvBuilder() {
		// TODO Auto-generated constructor stub
	}
	
	@BeforeClass
	static public void before() {
		if ( currentStage == null) {
			start();
		}
		mockTracer.reset();
	}
	
	@AfterClass
	static public void end() {
		if ( currentStage != null) {
			stop();
		}
	}
	
	static private void start() {
		
		if  (GlobalTracer.isRegistered())
			return;
		
	    // Create and bind to an orbit stage
		Builder builder = new Stage.Builder();
		builder.clusterName("orbit-unittest-cluster");
	    Stage stage = builder.build();
	
	    // Setup the tracer
	    GlobalTracer.register(mockTracer);
	    
	    TracedStage.enableTracing(stage);
	
	    Task<?> task = stage.start();
	    task.join();
	    stage.bind();		
	    
	    currentStage = stage;
	    
	    assertTrue(stage.getRuntime() != null);
	    assertTrue(GlobalTracer.isRegistered());
	}
	
	static private void stop() {
	
		if ( currentStage != null) {
			currentStage.stop().join();
		}
	}

	static public Scope activeScope(Span span) {
		return GlobalTracer.get().scopeManager().activate(span, false);
	}
}
