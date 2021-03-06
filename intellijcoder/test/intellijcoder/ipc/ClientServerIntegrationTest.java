package intellijcoder.ipc;

import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.client.contestant.ProblemModel;
import com.topcoder.client.contestant.RoomModel;
import com.topcoder.client.contestant.RoundModel;
import com.topcoder.client.contestant.view.LeaderListener;
import com.topcoder.client.contestant.view.PhaseListener;
import com.topcoder.client.contestant.view.RoomListListener;
import com.topcoder.client.contestant.view.RoundProblemsListener;
import com.topcoder.netCommon.contest.round.RoundProperties;
import com.topcoder.netCommon.contest.round.RoundType;
import com.topcoder.netCommon.contestantMessages.response.data.LeaderboardItem;
import com.topcoder.netCommon.contestantMessages.response.data.PhaseData;
import com.topcoder.shared.language.JavaLanguage;
import com.topcoder.shared.problem.DataType;
import com.topcoder.shared.problem.ProblemComponent;
import intellijcoder.arena.ArenaProcessLauncher;
import intellijcoder.arena.IntelliJCoderArenaPlugin;
import intellijcoder.workspace.WorkspaceManager;
import intellijcoder.main.IntelliJCoderException;
import intellijcoder.model.Problem;
import intellijcoder.model.TestCase;
import intellijcoder.os.Network;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static intellijcoder.model.ProblemMaker.*;
import static intellijcoder.util.TestUtil.assertExceptionMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Date: 14.01.11
 *
 * @author Konstantin Fadeyev
 */
@RunWith(JMock.class)
public class ClientServerIntegrationTest {
    public static final int TIMEOUT = 1000;
    private Mockery context = new JUnit4Mockery();


    @Test
    public void testProblemTransferFromClientToServer() throws Exception {
        FakeWorkspaceManager workspaceManager = new FakeWorkspaceManager();
        IntelliJCoderServer server = new IntelliJCoderServer(workspaceManager, new Network());

        int port = server.start();
        IntelliJCoderClient client = new IntelliJCoderClient(new Network(), port);

        Problem problem = sampleProgram();
        client.createProblemWorkspace(problem);

        workspaceManager.hasReceivedProblemEqualTo(problem);
    }

    @Test(timeout = TIMEOUT)
    public void testSourceTransferFromServerToClient() throws Exception {
        final WorkspaceManager workspaceManager = context.mock(WorkspaceManager.class);
        IntelliJCoderServer server = new IntelliJCoderServer(workspaceManager, new Network());

        int port = server.start();
        IntelliJCoderClient client = new IntelliJCoderClient(new Network(), port);

        context.checking(new Expectations(){{
            allowing(workspaceManager).getSolutionSource("className"); will(returnValue("solution source"));
        }});
        assertEquals("solution class source", "solution source", client.getSolutionSource("className"));
    }

    @Test(timeout = TIMEOUT)
    public void clientRethrowsWorkspaceCreationExceptionOccuredOnServer() throws Exception {
        final WorkspaceManager workspaceManager = context.mock(WorkspaceManager.class);

        IntelliJCoderServer server = new IntelliJCoderServer(workspaceManager, new Network());
        int port = server.start();
        IntelliJCoderClient client = new IntelliJCoderClient(new Network(), port);

        context.checking(new Expectations(){{
            allowing(workspaceManager).createProblemWorkspace(with(any(Problem.class)));
            will(throwException(new IntelliJCoderException("big error", null)));
        }});
        try {
            client.createProblemWorkspace(sampleProgram());
            fail("should rethrow exception");
        } catch (IntelliJCoderException e) {
            assertExceptionMessage(e, "big error");
        }
    }

    @Test(timeout = TIMEOUT)
    public void clientRethrowsGettingSourceExceptionOccuredOnServer() throws Exception {
        final WorkspaceManager workspaceManager = context.mock(WorkspaceManager.class);

        IntelliJCoderServer server = new IntelliJCoderServer(workspaceManager, new Network());
        int port = server.start();
        IntelliJCoderClient client = new IntelliJCoderClient(new Network(), port);

        context.checking(new Expectations(){{
            allowing(workspaceManager).getSolutionSource(with(any(String.class)));
            will(throwException(new IntelliJCoderException("big error", null)));
        }});
        try {
            client.getSolutionSource(someClassName());
            fail("should rethrow exception");
        } catch (IntelliJCoderException e) {
            assertExceptionMessage(e, "big error");
        }
    }

    @Test(timeout = TIMEOUT)
    public void severalRequestsToServer() throws Exception {
        final WorkspaceManager workspaceManager = context.mock(WorkspaceManager.class);
        IntelliJCoderServer server = new IntelliJCoderServer(workspaceManager, new Network());

        int port = server.start();
        IntelliJCoderClient client = new IntelliJCoderClient(new Network(), port);

        context.checking(new Expectations(){{
            allowing(workspaceManager).createProblemWorkspace(with(any(Problem.class)));
            allowing(workspaceManager).getSolutionSource("className");    will(returnValue("solution source"));
        }});
        client.createProblemWorkspace(sampleProgram());
        assertEquals("solution class source", "solution source", client.getSolutionSource("className"));
        assertEquals("solution class source requested 2nd time", "solution source", client.getSolutionSource("className"));
    }

    @Test
    public void testProblemTransferFromArenaPluginToIntelliJCoderServer() throws Exception {
        FakeWorkspaceManager workspaceManager = new FakeWorkspaceManager();
        IntelliJCoderServer server = new IntelliJCoderServer(workspaceManager, new Network());

        int port = server.start();

        setSystemProperty(ArenaProcessLauncher.INTELLIJCODER_PORT_PROPERTY, port);
        IntelliJCoderArenaPlugin plugin = new IntelliJCoderArenaPlugin();
        plugin.startUsing();


        final ProblemComponentModel inputComponentModel = context.mock(ProblemComponentModel.class);

        context.checking(new Expectations(){{
            allowing(inputComponentModel).getProblem(); will(returnValue(new ProblemModel() {
                public Long getProblemID() {
                    return null;
                }

                public RoundModel getRound() {
                    return new RoundModel() {
                        public int getRoundCategoryID() {
                            return 0;
                        }

                        public Long getRoundID() {
                            return null;
                        }

                        public String getContestName() {
                            return "SRM 144 DIV 1";
                        }

                        public String getRoundName() {
                            return null;
                        }

                        public String getDisplayName() {
                            return null;
                        }

                        public String getSingleName() {
                            return null;
                        }

                        public Integer getRoundTypeId() {
                            return null;
                        }

                        public RoundType getRoundType() {
                            return null;
                        }

                        public RoundProperties getRoundProperties() {
                            return null;
                        }

                        public Integer getPhase() {
                            return null;
                        }

                        public boolean getMenuStatus() {
                            return false;
                        }

                        public int getSecondsLeftInPhase() {
                            return 0;
                        }

                        public boolean isInChallengePhase() {
                            return false;
                        }

                        public void addPhaseListener(PhaseListener phaseListener) {

                        }

                        public void removePhaseListener(PhaseListener phaseListener) {

                        }

                        public boolean containsPhaseListener(PhaseListener phaseListener) {
                            return false;
                        }

                        public void addRoomListListener(RoomListListener roomListListener) {

                        }

                        public void removeRoomListListener(RoomListListener roomListListener) {

                        }

                        public void addRoundProblemsListener(RoundProblemsListener roundProblemsListener) {

                        }

                        public void removeRoundProblemsListener(RoundProblemsListener roundProblemsListener) {

                        }

                        public void addLeaderListener(LeaderListener leaderListener) {

                        }

                        public void removeLeaderListener(LeaderListener leaderListener) {

                        }

                        public boolean hasAdminRoom() {
                            return false;
                        }

                        public RoomModel getAdminRoom() {
                            return null;
                        }

                        public boolean hasCoderRooms() {
                            return false;
                        }

                        public RoomModel[] getCoderRooms() {
                            return new RoomModel[0];
                        }

                        public boolean hasProblems(Integer integer) {
                            return false;
                        }

                        public ProblemModel[] getProblems(Integer integer) {
                            return new ProblemModel[0];
                        }

                        public ProblemComponentModel[] getAssignedComponents(Integer integer) {
                            return new ProblemComponentModel[0];
                        }

                        public ProblemComponentModel getAssignedComponent(Integer integer, Long aLong) {
                            return null;
                        }

                        public ProblemModel getProblem(Integer integer, Long aLong) {
                            return null;
                        }

                        public ProblemComponentModel getComponent(Integer integer, Long aLong) {
                            return null;
                        }

                        public boolean hasLeaderboard() {
                            return false;
                        }

                        public LeaderboardItem[] getLeaderboard() {
                            return new LeaderboardItem[0];
                        }

                        public boolean hasSchedule() {
                            return false;
                        }

                        public PhaseData[] getSchedule() {
                            return new PhaseData[0];
                        }

                        public boolean isRoomLeader(String s) {
                            return false;
                        }

                        public RoomModel getRoomByCoder(String s) {
                            return null;
                        }

                        public boolean canDisplaySummary() {
                            return false;
                        }
                    };
                }

                public Integer getDivision() {
                    return null;
                }

                public Integer getProblemType() {
                    return null;
                }

                public String getName() {
                    return null;
                }

                public boolean hasComponents() {
                    return false;
                }

                public ProblemComponentModel[] getComponents() {
                    return new ProblemComponentModel[0];
                }

                public ProblemComponentModel getPrimaryComponent() {
                    return null;
                }

                public boolean hasIntro() {
                    return false;
                }

                public String getIntro() {
                    return null;
                }

                public boolean hasProblemStatement() {
                    return false;
                }

                public String getProblemStatement() {
                    return null;
                }

                public com.topcoder.shared.problem.Problem getProblem() {
                    return null;
                }

                public void addListener(Listener listener) {

                }

                public void removeListener(Listener listener) {

                }
            }
            ));
            allowing(inputComponentModel).getClassName(); will(returnValue("BinaryCode"));
            allowing(inputComponentModel).getReturnType();   will(returnValue(new DataType("int")));
            allowing(inputComponentModel).getMethodName();   will(returnValue("multiply"));
            allowing(inputComponentModel).getParamTypes();   will(returnValue(new DataType[0]));
            allowing(inputComponentModel).getParamNames();   will(returnValue(new String[0]));
            allowing(inputComponentModel).getComponent();   will(returnValue(new ProblemComponent() {
                 @Override
                 public int getMemLimitMB() {
                     return 256;
                 }

                 @Override
                 public int getExecutionTimeLimit() {
                     return 2000;
                 }
            }
            ));
            allowing(inputComponentModel).getTestCases();    will(returnValue(new com.topcoder.shared.problem.TestCase[] {new com.topcoder.shared.problem.TestCase(1, new String[0], "1", false)}));
        }});
        TestCase testCase = make(a(TestCase, with(input, new String[0]), with(output, "1")));
        Problem expectedProblem = make(a(Problem,
//                with(contestName, "SRM 144 DIV 1"),
                with(className, "BinaryCode"),
                with(returnType, "int"),
                with(methodName, "multiply"),
//                with(timeLimit, 2000),
//                with(memLimit, 256),
                with(testCases, new TestCase[]{ testCase })
        ));

        plugin.setProblemComponent(inputComponentModel, JavaLanguage.JAVA_LANGUAGE, null);
        workspaceManager.hasReceivedProblemEqualTo(expectedProblem);
    }


    private void setSystemProperty(String property, int value) {
        Properties properties = System.getProperties();
        properties.put(property, Integer.toString(value));
        System.setProperties(properties);
    }

    private Problem sampleProgram() {
        return make(a(Problem));
    }

    private String someClassName() {
        return "";
    }
}
