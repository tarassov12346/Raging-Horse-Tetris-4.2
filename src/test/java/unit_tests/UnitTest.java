package unit_tests;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.Stage;
import com.app.game.tetris.model.State;
import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(
        classes = {
                com.app.game.tetris.TetrisNewApplication.class,
                com.app.game.tetris.tetriserviceImpl.PlayGame.class,
                UnitTestService.class
        },
        properties = {"width=10",
                "height=20",
                "logging.file.name=target/logs/quality-automation.log",
                // 🔥 ДОБАВЛЕНО: Полностью отключаем сетевой клиент Eureka на время выполнения тестов
                "eureka.client.enabled=false"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UnitTest {

    @Autowired
    private UnitTestService unitTestService;

    @Autowired
    private PlayGameService playGameService;

    @MockitoBean
    private TaskScheduler taskScheduler;

    @MockitoBean
    private HazelcastInstance hazelcastInstance;

    // Фейковая In-Memory карта для эмуляции Hazelcast
    private final ConcurrentHashMap<String, State> fakeStorage = new ConcurrentHashMap<>();
    private IMap<String, State> mockMap;

    @BeforeAll
    public static void doBeforeTests() {
        log.info("UnitTests start");
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void initMocks() {
        log.info("Test Method is called. Initializing Hazelcast Mock Storage...");
        fakeStorage.clear();

        // 1. Создаем и обучаем фейковый IMap
        mockMap = (IMap<String, State>) Mockito.mock(IMap.class);

        Mockito.when(mockMap.get(Mockito.anyString())).thenAnswer(invocation ->
                fakeStorage.get(invocation.getArgument(0, String.class))
        );

        Mockito.doAnswer(invocation -> {
            fakeStorage.put(invocation.getArgument(0, String.class), invocation.getArgument(1, State.class));
            return null;
        }).when(mockMap).put(Mockito.anyString(), Mockito.any(State.class));

        // 2. Обучаем сам hazelcastInstance (на случай вызовов в коде)
        Mockito.<IMap<String, State>>when(hazelcastInstance.getMap("user-states")).thenReturn(mockMap);

        // 🔥 КРИТИЧЕСКИЙ ШАГ: Напрямую внедряем наш mockMap в приватное поле userStates сервиса PlayGame,
        // чтобы обойти проблему ранней инициализации в конструкторе!
        org.springframework.test.util.ReflectionTestUtils.setField(
                playGameService,
                "userStates",
                mockMap
        );
    }

    private static Stream<State> data() {
        UnitTestService utils = new UnitTestService();

        State stateWith2Rows = new State(
                utils.makeStageWith2FilledRows(),
                true,
                new Game("Tester", 0)
        );

        State stateWith3Rows = new State(
                utils.makeStageWith3FilledRows(),
                true,
                new Game("Tester", 0)
        );

        return Stream.of(stateWith2Rows, stateWith3Rows);
    }

    /**
     * Тест 1: Схлопывание заполненных рядов и начисление очков.
     */
    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Проверка схлопывания заполненных рядов тетриса")
    public void doFullRowsCollapseAndScoreIsUpdated(State state) {
        log.info("doFullRowsCollapseAndScoreIsUpdated Test start");
        log.info("filled rows number is {}", unitTestService.countFilledCells(state));

        State newState = playGameService.newTetraminoState(state).orElse(state);
        Tetramino tetramino = newState.getStage().getTetramino();

        int tetraminoX = newState.getStage().getTetraminoX();
        int tetraminoY = newState.getStage().getTetraminoY();
        int collapsedLayersCount = newState.getStage().getCollapsedLayersCount();

        log.info("collapsed layers count={}", collapsedLayersCount);
        log.info("players score ={}", newState.getGame().getPlayerScore());

        Stage expectedStage = unitTestService.makeStageWithOnlyLeftUnfilledRows(collapsedLayersCount);
        expectedStage.setTetramino(tetramino);
        expectedStage.setTetraminoX(tetraminoX);
        expectedStage.setTetraminoY(tetraminoY);

        State expectedState = playGameService.buildState(expectedStage, true, new Game("Tester", collapsedLayersCount * 10));

        Assertions.assertEquals(expectedState, newState);
    }

    /**
     * Тест 2: Проверка сдвига фигуры влево и остановки у края стакана.
     */
    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Проверка движения фигуры влево до упора")
    public void doesTetraminoMoveLeftStopAtBorder(State state) {
        log.info("doesTetraminoMoveLeftStopAtBorder Test start");
        String username = "Tester";

        State stateWithNewTetramino = playGameService.newTetraminoState(state).orElse(state);
        playGameService.setState(stateWithNewTetramino, username);

        int tetraminoY = stateWithNewTetramino.getStage().getTetraminoY();

        State newState = stateWithNewTetramino;
        for (int i = 0; i < 13; i++) {
            newState = playGameService.moveLeftState(username);
        }

        Tetramino tetramino = newState.getStage().getTetramino();
        int collapsedLayersCount = newState.getStage().getCollapsedLayersCount();

        // Физическая проверка: координата X зафиксировалась на валидном упоре стены (0 или -1 для I)
        int expectedX = newState.getStage().getTetraminoX();
        Assertions.assertTrue(expectedX == 0 || expectedX == -1, "Фигура вышла за левую границу стакана!");

        Stage expectedStage = unitTestService.makeStageWithOnlyLeftUnfilledRows(collapsedLayersCount);
        expectedStage.setTetramino(tetramino);
        expectedStage.setTetraminoX(expectedX);
        expectedStage.setTetraminoY(tetraminoY);

        State expectedState = playGameService.buildState(expectedStage, true, new Game(username, collapsedLayersCount * 10));
        Assertions.assertEquals(expectedState, newState);
    }

    /**
     * Тест 3: Проверка жесткого падения фигуры до упора вниз.
     */
    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Проверка падения фигуры до дна стакана")
    public void doesTetraminoMoveDownStopAtUnfilledLayers(State state) {
        log.info("doesTetraminoMoveDownStopAtUnfilledLayers Test start");
        String username = "Tester";

        State stateWithNewTetramino = playGameService.newTetraminoState(state).orElse(state);
        playGameService.setState(stateWithNewTetramino, username);

        int tetraminoX = stateWithNewTetramino.getStage().getTetraminoX();

        State newState = playGameService.dropDownState(username);
        Tetramino tetramino = newState.getStage().getTetramino();
        int collapsedLayersCount = newState.getStage().getCollapsedLayersCount();

        // Физическая проверка: фигура упала на дно (Y равен 15 или 16 в зависимости от геометрии блока)
        int expectedY = newState.getStage().getTetraminoY();
        Assertions.assertTrue(expectedY == 15 || expectedY == 16, "Фигура не долетела до дна стакана!");

        Stage expectedStage = unitTestService.makeStageWithOnlyLeftUnfilledRows(collapsedLayersCount);
        expectedStage.setTetramino(tetramino);
        expectedStage.setTetraminoX(tetraminoX);
        expectedStage.setTetraminoY(expectedY);

        State expectedState = playGameService.buildState(expectedStage, true, new Game(username, collapsedLayersCount * 10));
        Assertions.assertEquals(expectedState, newState);
    }

    /**
     * Тест 4: Проверка корректного вращения матрицы фигуры.
     * 🔥 ИСПРАВЛЕНО: Вызываем rotateState через интерфейс, предварительно сохранив сессию
     */
    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Проверка поворота фигуры по часовой стрелке")
    public void doesTetraminoRotate(State state) {
        log.info("doesTetraminoRotate Test start");
        String username = "Tester";

        State stateWithNewTetramino = playGameService.newTetraminoState(state).orElse(state);
        playGameService.setState(stateWithNewTetramino, username);

        int tetraminoX = stateWithNewTetramino.getStage().getTetraminoX();
        int tetraminoY = stateWithNewTetramino.getStage().getTetraminoY();

        // Поворачиваем фигуру через публичный интерфейсный метод rotateState
        State newState = playGameService.rotateState(username);

        char[][] rotatedMatrix = unitTestService.rotateMatrix(stateWithNewTetramino.getStage().getTetramino().getShape());
        Tetramino newTetramino = new Tetramino(rotatedMatrix);
        int collapsedLayersCount = newState.getStage().getCollapsedLayersCount();

        Stage expectedStage = unitTestService.makeStageWithOnlyLeftUnfilledRows(collapsedLayersCount);
        expectedStage.setTetramino(newTetramino);
        expectedStage.setTetraminoX(tetraminoX);
        expectedStage.setTetraminoY(tetraminoY);

        State expectedState = playGameService.buildState(expectedStage, true, new Game(username, collapsedLayersCount * 10));

        log.info("Tetramino initial shape {}", unitTestService.matrixToString(stateWithNewTetramino.getStage().getTetramino().getShape()));
        log.info("Tetramino after rotate new shape {}", unitTestService.matrixToString(newState.getStage().getTetramino().getShape()));

        Assertions.assertEquals(expectedState, newState);
    }

    @AfterEach
    public void doAfterEachTestMethod() {
        log.info("Test Method is finished");
    }

    @AfterAll
    public static void doAfterTests() {
        log.info("UnitTests are finished");
    }
}
