package com.cj.scmconduit.server;

import com.cj.scmconduit.server.session.CodeSubmissionSession;
import org.hamcrest.Matcher;
import org.httpobjects.Representation;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.path.PathParam;
import org.httpobjects.path.PathParamName;
import org.httpobjects.path.PathVariables;
import org.httpobjects.test.MockRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;

import static org.httpobjects.DSL.OK;
import static org.httpobjects.DSL.Text;
import static org.mockito.Mockito.times;

public class ConduitHttpResourceTest {

    @Test
    public void trashesSessionWhenFinished() {
        //given
        ConduitOrchestrator conduitOrchestrator = Mockito.mock(ConduitOrchestrator.class);
        CodeSubmissionSession codeSubmissionSession = Mockito.mock(CodeSubmissionSession.class);
        Mockito.when(conduitOrchestrator.getSessionWithId(11)).thenReturn(codeSubmissionSession);
        Mockito.when(codeSubmissionSession.state()).thenReturn(CodeSubmissionSession.State.FINISHED);

        ConduitHttpResource conduitHttpResource = new ConduitHttpResource("whatever", conduitOrchestrator);

        Request req = new MockRequest(conduitHttpResource, "asdf") {
            @Override
            public PathVariables pathVars() {
                return new PathVariables(new PathParam(new PathParamName("remainder"), "11"));
            }
        };

        //when
        conduitHttpResource.get(req);

        //then
        Mockito.verify(codeSubmissionSession, times(1)).trash();
    }
    @Test
    public void doesNotTrashSessionWhenAlreadyTrashed() {
        //given
        ConduitOrchestrator conduitOrchestrator = Mockito.mock(ConduitOrchestrator.class);
        CodeSubmissionSession codeSubmissionSession = Mockito.mock(CodeSubmissionSession.class);
        Mockito.when(conduitOrchestrator.getSessionWithId(11)).thenReturn(codeSubmissionSession);
        Mockito.when(codeSubmissionSession.state()).thenReturn(CodeSubmissionSession.State.TRASHY);

        ConduitHttpResource conduitHttpResource = new ConduitHttpResource("whatever", conduitOrchestrator);

        Request req = new MockRequest(conduitHttpResource, "whatever") {
            @Override
            public PathVariables pathVars() {
                return new PathVariables(new PathParam(new PathParamName("remainder"), "11"));
            }
        };

        //when
        conduitHttpResource.get(req);

        //then
        Mockito.verify(codeSubmissionSession, times(0)).trash();
    }

    @Test
    public void trashedAreOkay() {
        //given
        ConduitOrchestrator conduitOrchestrator = Mockito.mock(ConduitOrchestrator.class);
        CodeSubmissionSession codeSubmissionSession = Mockito.mock(CodeSubmissionSession.class);
        Mockito.when(conduitOrchestrator.getSessionWithId(11)).thenReturn(codeSubmissionSession);
        Mockito.when(codeSubmissionSession.state()).thenReturn(CodeSubmissionSession.State.TRASHY);
        Mockito.when(codeSubmissionSession.hadErrors()).thenReturn(false);
        Mockito.when(codeSubmissionSession.explanation()).thenReturn("testWorked");
        ConduitHttpResource conduitHttpResource = new ConduitHttpResource("whatever", conduitOrchestrator);

        Request req = new MockRequest(conduitHttpResource, "whatever") {
            @Override
            public PathVariables pathVars() {
                return new PathVariables(new PathParam(new PathParamName("remainder"), "11"));
            }
        };

        //when
        Response response = conduitHttpResource.get(req);

        //then
        Assert.assertEquals(response.code().value(), 200);
        Assert.assertEquals("OK:testWorked", responseAsString(response)); 
    }

    private String responseAsString(Response response) {
        ByteArrayOutputStream boos = new ByteArrayOutputStream();
        response.representation().write(boos);
        String responseString = new String(boos.toByteArray());
        return responseString;
    }
}