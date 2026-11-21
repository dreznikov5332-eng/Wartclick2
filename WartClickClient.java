package com.wartclick;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1688;
import net.minecraft.class_1799;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_2421;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_638;
import net.minecraft.class_746;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WartClickClient implements ClientModInitializer {
    // ---------- Сбор урожая ----------
    /** Собирать адский нарост (стадия 3 из 3). */
    private static final boolean HARVEST_WART = true;
    /** Собирать морковь (стадия 7 из 7). */
    private static final boolean HARVEST_CARROTS = true;
    /** Кликать только с мотыгой в руке (false = кликать с любым предметом). */
    private static final boolean REQUIRE_HOE = true;

    // ---------- Наведение камеры ----------
    /** Наводить камеру на культуру перед кликом (false = максимум скорости, без камеры). */
    private static final boolean AIM_AT_CROPS = true;
    /** Максимальный поворот камеры за один тик (градусов). 180 = всегда успевает за один тик. */
    private static final float MAX_AIM_TURN = 180.0F;
    /** Кликаем по всем культурам, которые от центра прицела не дальше этого угла (градусов). */
    private static final double CLICK_TOLERANCE = 25.0;

    /** Сколько блоков проверять вперёд по ходу движения. */
    private static final int FORWARD = 4;
    /** Сколько блоков проверять назад. */
    private static final int BACK = 2;
    /** Сколько блоков проверять в каждую сторону (влево и вправо). */
    private static final int SIDE = 3;
    /** Сколько блоков проверять вверх. */
    private static final int VERT_UP = 5;
    /** Сколько блоков проверять вниз. */
    private static final int VERT_DOWN = 1;
    /** Максимум кликов за один тик. */
    private static final int MAX_CLICKS_PER_TICK = 16;
    /** Через сколько тиков можно снова кликнуть по той же культуре. */
    private static final int SAME_POS_COOLDOWN = 2;
    private static final boolean REQUIRE_LINE_OF_SIGHT = true;
    private static final boolean DEBUG = true;
    private static final double[] GRID = {0.5, 0.05, 0.95};
    /** Допуск видимости: чем больше, тем больше блоков за краем яруса берётся. */
    private static final double GRAZE = 1.6;

    // ---------- Режим «по списку» (свои точки) ----------
    /** Запускаться сразу в режиме «по списку» (M переключает на автопоиск и обратно). */
    private static final boolean START_IN_LIST_MODE = true;
    /** Кликать только по созревшим культурам (false = кликать по каждой точке в любом случае). */
    private static final boolean LIST_ONLY_RIPE = true;
    /** После последней точки начинать список заново. */
    private static final boolean LIST_LOOP = true;
    /** Пауза между кликами (тиков). 0 = по одной точке за тик. */
    private static final int LIST_DELAY_TICKS = 0;
    /** Насколько точно навестись на точку перед кликом (градусов). */
    private static final double LIST_TOLERANCE = 2.0;
    /** Если за столько тиков не удалось навестись на точку, пропускаем её. */
    private static final int LIST_AIM_TIMEOUT = 20;
    /** Через сколько тиков можно снова кликнуть по той же точке. */
    private static final int LIST_SAME_POS_COOLDOWN = 4;
    /** Записывать только блоки моркови / нароста (false = любой блок под прицелом). */
    private static final boolean RECORD_ONLY_CROPS = true;
    /** Сколько тиков можно подтвердить очистку списка вторым нажатием. */
    private static final int CLEAR_CONFIRM_TICKS = 60;

    // ---------- Автопилот вагонетки ----------
    /** Разгонять ли вагонетку клавишей A/D, когда она стоит. Пока false: мод только садится. */
    private static final boolean AUTO_PUSH = false;
    /** Какой клавишей разгонять вагонетку: true = D (вправо), false = A (влево). */
    private static final boolean PUSH_RIGHT = true;
    /** Показывать над хотбаром, видит ли мод вагонетку. */
    private static final boolean AUTO_DEBUG = true;
    /** Радиус, в котором мод садится в вагонетку (блоков). */
    private static final double MOUNT_RANGE = 2.8;
    /** Пауза между попытками сесть (тиков). */
    private static final int MOUNT_RETRY_TICKS = 10;
    /** Если скорость ниже этого (блоков за тик) дольше STALL_TICKS, начинаем разгонять. */
    private static final double STALL_SPEED = 0.05;
    private static final int STALL_TICKS = 10;
    /** Разгоняем клавишей, пока скорость не дойдёт до этого значения (0.4 = максимум обычной вагонетки). */
    private static final double TARGET_SPEED = 0.3;
    /** После нажатия Shift не садиться обратно столько тиков. */
    private static final int SNEAK_SUPPRESS_TICKS = 60;

    // ---------- Поход к вагонетке ----------
    /** Включить самостоятельный поход к далёкой вагонетке. */
    private static final boolean WALK_TO_CART = true;
    /** Как далеко искать вагонетку (блоков). */
    private static final double SEARCH_RANGE = 96.0;
    /** Максимальный поворот камеры за тик при ходьбе (градусов). */
    private static final float MAX_TURN = 30.0F;
    /** Наклон камеры при ходьбе (градусов, 0 = горизонт). */
    private static final float WALK_PITCH = 10.0F;
    /** Проверка «застряли ли» раз в столько тиков. */
    private static final int STUCK_CHECK_TICKS = 40;
    /** Минимальное приближение за этот период (блоков), иначе считаем, что застряли. */
    private static final double STUCK_MIN_PROGRESS = 1.0;
    /** Сколько проверок подряд без прогресса, чтобы сдаться. */
    private static final int MAX_STUCK = 3;
    /** Сколько тиков ждать после отказа (опасность или застряли). */
    private static final int GIVE_UP_TICKS = 200;

    private static class_304 toggleKey;
    private static class_304 autoRideKey;
    private static class_304 addPointKey;
    private static class_304 undoPointKey;
    private static class_304 clearPointsKey;
    private static class_304 modeKey;

    private static class Candidate {
        final class_2338 pos;
        final class_243 hit;
        double err;

        Candidate(class_2338 pos, class_243 hit) {
            this.pos = pos;
            this.hit = hit;
        }
    }

    // сбор
    private boolean enabled = false;
    private int tickCounter = 0;
    private class_2350 forwardDir = null;
    private final Map<class_2338, Integer> recent = new HashMap<>();

    private final Set<class_2338> dbgSeen = new HashSet<>();
    private final Set<class_2338> dbgFar = new HashSet<>();
    private final Set<class_2338> dbgBlocked = new HashSet<>();
    private final Set<class_2338> dbgClicked = new HashSet<>();
    private boolean dbgNoHoe = false;
    private int dbgAllCrops = 0;

    // список точек
    private final List<class_2338> points = new ArrayList<>();
    private final Map<class_2338, Integer> listRecent = new HashMap<>();
    private boolean listMode = START_IN_LIST_MODE;
    private int listIndex = 0;
    private int listWait = 0;
    private int aimTicks = 0;
    private int listClicks = 0;
    private int clock = 0;
    private int clearConfirmUntil = -1;

    // автопилот
    private boolean autoRide = false;
    private int autoTick = 0;
    private int lastMountTry = -1000;
    private int suppressUntil = 0;
    private class_243 lastCartPos = null;
    private int stallTicks = 0;
    private boolean pushing = false;
    private boolean pressedByUs = false;

    // поход
    private boolean walkKeysDown = false;
    private Float savedYaw = null;
    private Float savedPitch = null;
    private double walkLastDist = -1;
    private int walkCheckTick = 0;
    private int stuckCount = 0;
    private int walkPauseUntil = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.wartclick.toggle",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_B,
                class_304.class_11900.field_62556
        ));
        autoRideKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.wartclick.autoride",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_N,
                class_304.class_11900.field_62556
        ));

        addPointKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.wartclick.addpoint",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_V,
                class_304.class_11900.field_62556
        ));
        undoPointKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.wartclick.undopoint",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_Z,
                class_304.class_11900.field_62556
        ));
        clearPointsKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.wartclick.clearpoints",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_X,
                class_304.class_11900.field_62556
        ));
        modeKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.wartclick.mode",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_M,
                class_304.class_11900.field_62556
        ));

        loadPoints();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(class_310 client) {
        class_746 player = client.field_1724;

        while (toggleKey.method_1436()) {
            enabled = !enabled;
            recent.clear();
            listRecent.clear();
            listIndex = 0;
            listWait = 0;
            aimTicks = 0;
            forwardDir = null;
            if (player != null) {
                player.method_7353(class_2561.method_43470("WartClick: " + (enabled ? "ВКЛ" : "ВЫКЛ")), true);
            }
        }
        clock++;
        while (modeKey.method_1436()) {
            listMode = !listMode;
            listIndex = 0;
            listWait = 0;
            aimTicks = 0;
            recent.clear();
            listRecent.clear();
            msg(player, "WartClick: режим — " + (listMode
                    ? "ПО СПИСКУ (точек: " + points.size() + ")"
                    : "АВТОПОИСК"));
        }
        while (addPointKey.method_1436()) {
            addPoint(client, player);
        }
        while (undoPointKey.method_1436()) {
            if (points.isEmpty()) {
                msg(player, "WartClick: список пуст");
            } else {
                class_2338 removed = points.remove(points.size() - 1);
                listIndex = 0;
                savePoints();
                msg(player, "WartClick: удалена точка #" + (points.size() + 1)
                        + " (" + fmt(removed) + "), осталось: " + points.size());
            }
        }
        while (clearPointsKey.method_1436()) {
            if (points.isEmpty()) {
                msg(player, "WartClick: список и так пуст");
            } else if (clock <= clearConfirmUntil) {
                points.clear();
                listIndex = 0;
                clearConfirmUntil = -1;
                savePoints();
                msg(player, "WartClick: список очищен");
            } else {
                clearConfirmUntil = clock + CLEAR_CONFIRM_TICKS;
                msg(player, "WartClick: нажми ещё раз, чтобы очистить " + points.size() + " точек");
            }
        }
        while (autoRideKey.method_1436()) {
            autoRide = !autoRide;
            if (!autoRide) {
                releasePush(client);
                releaseWalk(client);
            }
            if (player != null) {
                player.method_7353(class_2561.method_43470("Автопилот вагонетки: " + (autoRide ? "ВКЛ" : "ВЫКЛ")), true);
            }
        }

        autoRideTick(client);

        if (!enabled) return;
        if (player == null || client.field_1687 == null || client.field_1761 == null) return;
        if (client.field_1755 != null) return;

        tickCounter++;
        if (listMode) {
            listTick(client, player);
            return;
        }
        recent.entrySet().removeIf(e -> tickCounter - e.getValue() > SAME_POS_COOLDOWN);

        if (DEBUG && tickCounter % 20 == 0) {
            String msg = dbgNoHoe
                    ? "WartClick: в руке НЕ мотыга"
                    : "WartClick | в зоне культур: " + dbgAllCrops
                    + " | зрелых: " + dbgSeen.size()
                    + " | далеко: " + dbgFar.size()
                    + " | закрыто: " + dbgBlocked.size()
                    + " | клик: " + dbgClicked.size();
            player.method_7353(class_2561.method_43470(msg), true);
            dbgSeen.clear();
            dbgFar.clear();
            dbgBlocked.clear();
            dbgClicked.clear();
            dbgAllCrops = 0;
            dbgNoHoe = false;
        }

        class_1799 held = player.method_6047();
        if (REQUIRE_HOE && !held.method_31573(class_3489.field_42613)) {
            dbgNoHoe = true;
            return;
        }

        updateForward(player);

        class_2350 fwd = forwardDir;
        class_2350 right = fwd.method_10170();
        int fx = fwd.method_10148(), fz = fwd.method_10165();
        int rx = right.method_10148(), rz = right.method_10165();

        class_243 eye = player.method_33571();
        double reach = player.method_55754();
        double reachSq = reach * reach;
        class_2338 center = player.method_24515();

        // 1. Собираем всех кандидатов: зрелые, в досягаемости, видимые.
        List<Candidate> cands = new ArrayList<>();
        scan:
        for (int f = FORWARD; f >= -BACK; f--) {
            for (int s = -SIDE; s <= SIDE; s++) {
                for (int dy = VERT_UP; dy >= -VERT_DOWN; dy--) {
                    if (cands.size() >= 64) break scan;

                    class_2338 pos = center.method_10069(fx * f + rx * s, dy, fz * f + rz * s);
                    class_2680 state = client.field_1687.method_8320(pos);
                    if (!isCrop(state)) continue;
                    dbgAllCrops++;
                    if (!isRipe(state)) continue;
                    dbgSeen.add(pos);
                    if (distToBlockSq(eye, pos) > reachSq) {
                        dbgFar.add(pos);
                        continue;
                    }
                    if (recent.containsKey(pos)) continue;

                    class_243 hitVec = REQUIRE_LINE_OF_SIGHT
                            ? findVisiblePoint(client, player, eye, pos, reachSq)
                            : class_243.method_24953(pos);
                    if (hitVec == null) {
                        dbgBlocked.add(pos);
                        continue;
                    }
                    cands.add(new Candidate(pos, hitVec));
                }
            }
        }
        if (cands.isEmpty()) return;

        // 2. Наводим камеру на ближайшую к прицелу культуру.
        if (AIM_AT_CROPS) {
            float curYaw = player.method_36454();
            float curPitch = player.method_36455();
            Candidate target = null;
            double bestErr = Double.MAX_VALUE;
            for (Candidate c : cands) {
                c.err = angularError(eye, c.hit, curYaw, curPitch);
                if (c.err < bestErr) {
                    bestErr = c.err;
                    target = c;
                }
            }
            if (bestErr > CLICK_TOLERANCE) {
                float dyaw = class_3532.method_15393(yawTo(eye, target.hit) - curYaw);
                float dpitch = pitchTo(eye, target.hit) - curPitch;
                float len = (float) Math.sqrt(dyaw * dyaw + dpitch * dpitch);
                if (len > MAX_AIM_TURN) {
                    float k = MAX_AIM_TURN / len;
                    dyaw *= k;
                    dpitch *= k;
                }
                player.method_36456(curYaw + dyaw);
                player.method_36457(class_3532.method_15363(curPitch + dpitch, -90.0F, 90.0F));
            }
        }

        // 3. Кликаем по тем, что сейчас под прицелом.
        float ny = player.method_36454();
        float np = player.method_36455();
        int clicks = 0;
        for (Candidate c : cands) {
            if (clicks >= MAX_CLICKS_PER_TICK) break;
            if (AIM_AT_CROPS && angularError(eye, c.hit, ny, np) > CLICK_TOLERANCE) continue;

            class_243 toEye = eye.method_1020(c.hit);
            class_2350 face = class_2350.method_10142(toEye.field_1352, toEye.field_1351, toEye.field_1350);
            class_3965 hit = new class_3965(c.hit, face, c.pos, false);
            class_1269 result = client.field_1761.method_2896(player, class_1268.field_5808, hit);
            if (result.method_23665()) {
                player.method_6104(class_1268.field_5808);
            }
            recent.put(c.pos, tickCounter);
            dbgClicked.add(c.pos);
            clicks++;
        }
    }

    // ================= Режим «по списку» =================

    private void listTick(class_310 client, class_746 player) {
        if (DEBUG && tickCounter % 20 == 0) listDebug(client, player);

        int n = points.size();
        if (n == 0) return;
        if (REQUIRE_HOE && !player.method_6047().method_31573(class_3489.field_42613)) return;

        listRecent.entrySet().removeIf(e -> tickCounter - e.getValue() > LIST_SAME_POS_COOLDOWN);
        if (listWait > 0) {
            listWait--;
            return;
        }
        if (LIST_LOOP && listIndex >= n) listIndex = 0;

        class_243 eye = player.method_33571();
        double reach = player.method_55754();
        double reachSq = reach * reach;

        // 1. Ищем следующую по порядку точку, по которой можно кликнуть.
        int found = -1;
        class_243 hitVec = null;
        for (int step = 0; step < n; step++) {
            int i = listIndex + step;
            if (i >= n) {
                if (!LIST_LOOP) break;
                i -= n;
            }
            class_2338 pos = points.get(i);
            if (listRecent.containsKey(pos)) continue;
            class_2680 state = client.field_1687.method_8320(pos);
            if (LIST_ONLY_RIPE && !(isCrop(state) && isRipe(state))) continue;
            if (distToBlockSq(eye, pos) > reachSq) continue;
            class_243 h = REQUIRE_LINE_OF_SIGHT
                    ? findVisiblePoint(client, player, eye, pos, reachSq)
                    : class_243.method_24953(pos);
            if (h == null) continue;
            found = i;
            hitVec = h;
            break;
        }
        if (found < 0) {
            aimTicks = 0;
            return;
        }
        class_2338 target = points.get(found);

        // 2. Наводим камеру на эту точку.
        float curYaw = player.method_36454();
        float curPitch = player.method_36455();
        if (angularError(eye, hitVec, curYaw, curPitch) > LIST_TOLERANCE) {
            float dyaw = class_3532.method_15393(yawTo(eye, hitVec) - curYaw);
            float dpitch = pitchTo(eye, hitVec) - curPitch;
            float len = (float) Math.sqrt(dyaw * dyaw + dpitch * dpitch);
            if (len > MAX_AIM_TURN) {
                float k = MAX_AIM_TURN / len;
                dyaw *= k;
                dpitch *= k;
            }
            player.method_36456(curYaw + dyaw);
            player.method_36457(class_3532.method_15363(curPitch + dpitch, -90.0F, 90.0F));
        }
        if (angularError(eye, hitVec, player.method_36454(), player.method_36455()) > LIST_TOLERANCE) {
            // Ещё не навелись; если слишком долго не выходит, пропускаем точку.
            if (++aimTicks > LIST_AIM_TIMEOUT) {
                listIndex = found + 1;
                aimTicks = 0;
            }
            return;
        }

        // 3. Кликаем и переходим к следующей точке.
        class_243 toEye = eye.method_1020(hitVec);
        class_2350 face = class_2350.method_10142(toEye.field_1352, toEye.field_1351, toEye.field_1350);
        class_3965 hit = new class_3965(hitVec, face, target, false);
        class_1269 result = client.field_1761.method_2896(player, class_1268.field_5808, hit);
        if (result.method_23665()) {
            player.method_6104(class_1268.field_5808);
        }
        listRecent.put(target, tickCounter);
        listClicks++;
        listIndex = found + 1;
        listWait = LIST_DELAY_TICKS;
        aimTicks = 0;
    }

    private void listDebug(class_310 client, class_746 player) {
        String msg;
        if (points.isEmpty()) {
            msg = "WartClick [список]: пусто. Наведись на морковь/нарост и нажми V";
        } else if (REQUIRE_HOE && !player.method_6047().method_31573(class_3489.field_42613)) {
            msg = "WartClick: в руке НЕ мотыга";
        } else {
            class_243 eye = player.method_33571();
            double reach = player.method_55754();
            double reachSq = reach * reach;
            int ripe = 0, far = 0, blocked = 0;
            for (class_2338 pos : points) {
                class_2680 st = client.field_1687.method_8320(pos);
                if (!(isCrop(st) && isRipe(st))) continue;
                ripe++;
                if (distToBlockSq(eye, pos) > reachSq) {
                    far++;
                } else if (REQUIRE_LINE_OF_SIGHT && findVisiblePoint(client, player, eye, pos, reachSq) == null) {
                    blocked++;
                }
            }
            int next = Math.min(listIndex, points.size() - 1) + 1;
            msg = "WartClick [список] | точек: " + points.size()
                    + " | следующая: #" + next
                    + " | зрелых: " + ripe
                    + " | далеко: " + far
                    + " | закрыто: " + blocked
                    + " | клик: " + listClicks;
        }
        listClicks = 0;
        player.method_7353(class_2561.method_43470(msg), true);
    }

    /** Записывает блок под прицелом в конец списка. */
    private void addPoint(class_310 client, class_746 player) {
        if (player == null || client.field_1687 == null) return;

        class_243 eye = player.method_33571();
        class_243 look = class_243.method_1030(player.method_36455(), player.method_36454());
        double reach = player.method_55754();
        class_243 end = new class_243(
                eye.field_1352 + look.field_1352 * reach,
                eye.field_1351 + look.field_1351 * reach,
                eye.field_1350 + look.field_1350 * reach);
        class_239 r = client.field_1687.method_17742(new class_3959(
                eye, end,
                class_3959.class_3960.field_17558,
                class_3959.class_242.field_1348,
                player));
        if (!(r instanceof class_3965 bhr) || r.method_17783() == class_239.class_240.field_1333) {
            msg(player, "WartClick: наведи прицел на блок культуры");
            return;
        }

        class_2338 hitPos = bhr.method_17777();
        class_2338 pos = new class_2338(hitPos.method_10263(), hitPos.method_10264(), hitPos.method_10260());
        if (!isCrop(client.field_1687.method_8320(pos))) {
            // Целились в грядку под растением: берём растение сверху.
            class_2338 above = pos.method_10069(0, 1, 0);
            if (isCrop(client.field_1687.method_8320(above))) {
                pos = above;
            } else if (RECORD_ONLY_CROPS) {
                msg(player, "WartClick: это не морковь/нарост (" + fmt(pos) + ")");
                return;
            }
        }

        int existing = points.indexOf(pos);
        if (existing >= 0) {
            msg(player, "WartClick: этот блок уже в списке (#" + (existing + 1) + ")");
            return;
        }
        points.add(pos);
        savePoints();
        msg(player, "WartClick: точка #" + points.size() + " — " + fmt(pos));
    }

    private static String fmt(class_2338 p) {
        return p.method_10263() + " " + p.method_10264() + " " + p.method_10260();
    }

    private static void msg(class_746 player, String text) {
        if (player != null) player.method_7353(class_2561.method_43470(text), true);
    }

    private static Path pointsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("wartclick-points.txt");
    }

    private void savePoints() {
        List<String> lines = new ArrayList<>();
        for (class_2338 p : points) lines.add(fmt(p));
        try {
            Files.write(pointsFile(), lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private void loadPoints() {
        Path f = pointsFile();
        if (!Files.exists(f)) return;
        try {
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String[] t = line.trim().split("\\s+");
                if (t.length != 3) continue;
                try {
                    class_2338 p = new class_2338(Integer.parseInt(t[0]), Integer.parseInt(t[1]), Integer.parseInt(t[2]));
                    if (!points.contains(p)) points.add(p);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    // ================= Углы =================

    private static float yawTo(class_243 eye, class_243 p) {
        return (float) (class_3532.method_15349(p.field_1350 - eye.field_1350, p.field_1352 - eye.field_1352) * 57.29577951308232) - 90.0F;
    }

    private static float pitchTo(class_243 eye, class_243 p) {
        double dx = p.field_1352 - eye.field_1352;
        double dz = p.field_1350 - eye.field_1350;
        return (float) (-(class_3532.method_15349(p.field_1351 - eye.field_1351, Math.sqrt(dx * dx + dz * dz)) * 57.29577951308232));
    }

    private static double angularError(class_243 eye, class_243 p, float yaw, float pitch) {
        float dyaw = class_3532.method_15393(yawTo(eye, p) - yaw);
        float dpitch = pitchTo(eye, p) - pitch;
        return Math.sqrt(dyaw * dyaw + dpitch * dpitch);
    }

    // ================= Какие культуры собираем =================

    private static boolean isCrop(class_2680 state) {
        return (HARVEST_WART && state.method_27852(class_2246.field_9974))
                || (HARVEST_CARROTS && state.method_27852(class_2246.field_10609));
    }

    private static boolean isRipe(class_2680 state) {
        if (state.method_27852(class_2246.field_9974)) {
            return state.method_11654(class_2421.field_11306) >= 3;
        }
        if (state.method_27852(class_2246.field_10609)) {
            return state.method_11654(class_2741.field_12550) >= 7;
        }
        return false;
    }

    // ================= Автопилот вагонетки =================

    private void autoRideTick(class_310 client) {
        class_746 player = client.field_1724;
        if (!autoRide || player == null || client.field_1687 == null
                || client.field_1761 == null || client.field_1755 != null) {
            releasePush(client);
            releaseWalk(client);
            return;
        }

        autoTick++;
        if (player.method_5715()) {
            suppressUntil = autoTick + SNEAK_SUPPRESS_TICKS;
        }

        class_1297 vehicle = player.method_5854();
        if (vehicle instanceof class_1688 cart) {
            // Сели: останавливаем ходьбу и возвращаем камеру, какой она была.
            releaseWalk(client);
            if (savedYaw != null) {
                player.method_36456(savedYaw);
                player.method_36457(savedPitch);
                savedYaw = null;
                savedPitch = null;
            }
            walkLastDist = -1;
            stuckCount = 0;

            class_243 pos = new class_243(cart.method_23317(), cart.method_23318(), cart.method_23321());
            double speed = 0;
            if (lastCartPos != null) {
                double dx = pos.field_1352 - lastCartPos.field_1352;
                double dz = pos.field_1350 - lastCartPos.field_1350;
                speed = Math.sqrt(dx * dx + dz * dz);
            }
            lastCartPos = pos;

            if (speed < STALL_SPEED) stallTicks++; else stallTicks = 0;
            if (!pushing && stallTicks >= STALL_TICKS) pushing = true;
            if (pushing && speed >= TARGET_SPEED) pushing = false;

            if (pushing && AUTO_PUSH) pushOn(client); else releasePush(client);
            return;
        }

        // Не в вагонетке: сбрасываем состояние.
        lastCartPos = null;
        stallTicks = 0;
        pushing = false;
        releasePush(client);

        if (autoTick < suppressUntil) {
            releaseWalk(client);
            return;
        }

        // Ищем ближайшую вагонетку.
        class_1688 best = null;
        double bestD = Double.MAX_VALUE;
        for (class_1688 c : client.field_1687.method_8390(
                class_1688.class,
                player.method_5829().method_1014(SEARCH_RANGE),
                e -> true)) {
            double d = player.method_5858(c);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }

        if (AUTO_DEBUG && autoTick % 20 == 10) {
            player.method_7353(class_2561.method_43470(best == null
                    ? "Автопилот: вагонеток не видно"
                    : "Автопилот: вагонетка в " + String.format("%.1f", Math.sqrt(bestD)) + " блоках"), true);
        }

        if (best == null) {
            releaseWalk(client);
            return;
        }

        if (bestD <= MOUNT_RANGE * MOUNT_RANGE) {
            releaseWalk(client);

            // Смотрим на вагонетку и кликаем, как это делает игрок.
            if (savedYaw == null) {
                savedYaw = player.method_36454();
                savedPitch = player.method_36455();
            }
            class_243 eye = player.method_33571();
            double dx = best.method_23317() - eye.field_1352;
            double dy = (best.method_23318() + 0.4) - eye.field_1351;
            double dz = best.method_23321() - eye.field_1350;
            double h = Math.sqrt(dx * dx + dz * dz);
            player.method_36456((float) (class_3532.method_15349(dz, dx) * 57.29577951308232) - 90.0F);
            player.method_36457((float) (-(class_3532.method_15349(dy, h) * 57.29577951308232)));

            if (autoTick - lastMountTry >= MOUNT_RETRY_TICKS) {
                lastMountTry = autoTick;
                client.field_1761.method_2905(player, best, class_1268.field_5808);
            }
        } else if (WALK_TO_CART) {
            walkToward(client, player, best);
        }
    }

    private void walkToward(class_310 client, class_746 player, class_1688 cart) {
        if (autoTick < walkPauseUntil) {
            releaseWalk(client);
            return;
        }

        if (savedYaw == null) {
            savedYaw = player.method_36454();
            savedPitch = player.method_36455();
            walkLastDist = -1;
            walkCheckTick = autoTick;
            stuckCount = 0;
        }

        double dx = cart.method_23317() - player.method_23317();
        double dz = cart.method_23321() - player.method_23321();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Плавный поворот к вагонетке.
        float targetYaw = (float) (class_3532.method_15349(dz, dx) * 57.29577951308232) - 90.0F;
        float diff = class_3532.method_15393(targetYaw - player.method_36454());
        float step = class_3532.method_15363(diff, -MAX_TURN, MAX_TURN);
        player.method_36456(player.method_36454() + step);
        player.method_36457(WALK_PITCH);

        // Обрыв или лава впереди: стоп.
        if (isDangerAhead(client, player)) {
            giveUp(client, player, "Автопилот: впереди обрыв или лава, остановился");
            return;
        }

        // Застряли: не приближаемся.
        if (autoTick - walkCheckTick >= STUCK_CHECK_TICKS) {
            if (walkLastDist >= 0 && walkLastDist - dist < STUCK_MIN_PROGRESS) {
                stuckCount++;
            } else {
                stuckCount = 0;
            }
            walkLastDist = dist;
            walkCheckTick = autoTick;
            if (stuckCount >= MAX_STUCK) {
                giveUp(client, player, "Автопилот: не могу дойти до вагонетки");
                return;
            }
        } else if (walkLastDist < 0) {
            walkLastDist = dist;
        }

        client.field_1690.field_1894.method_23481(true);
        client.field_1690.field_1867.method_23481(true);
        client.field_1690.field_1903.method_23481(player.field_5976);
        walkKeysDown = true;
    }

    private void giveUp(class_310 client, class_746 player, String message) {
        releaseWalk(client);
        walkPauseUntil = autoTick + GIVE_UP_TICKS;
        walkLastDist = -1;
        stuckCount = 0;
        player.method_7353(class_2561.method_43470(message), true);
    }

    /** Впереди нет пола в пределах 3 блоков или есть лава. */
    private boolean isDangerAhead(class_310 client, class_746 player) {
        class_638 w = client.field_1687;
        class_243 look = class_243.method_1030(0.0F, player.method_36454());
        class_2338 ahead = class_2338.method_49637(
                player.method_23317() + look.field_1352 * 1.5,
                player.method_23318(),
                player.method_23321() + look.field_1350 * 1.5);

        boolean floor = false;
        for (int dy = 0; dy >= -3; dy--) {
            class_2338 p = ahead.method_10069(0, dy, 0);
            if (w.method_8316(p).method_15767(class_3486.field_15518)) return true;
            if (dy < 0 && !w.method_8320(p).method_26220(w, p).method_1110()) {
                floor = true;
            }
        }
        return !floor;
    }

    private void releaseWalk(class_310 client) {
        if (!walkKeysDown) return;
        client.field_1690.field_1894.method_23481(false);
        client.field_1690.field_1867.method_23481(false);
        client.field_1690.field_1903.method_23481(false);
        walkKeysDown = false;
    }

    private void pushOn(class_310 client) {
        class_304 k = PUSH_RIGHT ? client.field_1690.field_1849 : client.field_1690.field_1913;
        k.method_23481(true);
        pressedByUs = true;
    }

    private void releasePush(class_310 client) {
        if (!pressedByUs) return;
        class_304 k = PUSH_RIGHT ? client.field_1690.field_1849 : client.field_1690.field_1913;
        k.method_23481(false);
        pressedByUs = false;
    }

    // ================= Сбор урожая: вспомогательные методы =================

    private static double distToBlockSq(class_243 eye, class_2338 pos) {
        double dx = Math.max(Math.max(pos.method_10263() - eye.field_1352, 0), eye.field_1352 - (pos.method_10263() + 1));
        double dy = Math.max(Math.max(pos.method_10264() - eye.field_1351, 0), eye.field_1351 - (pos.method_10264() + 1));
        double dz = Math.max(Math.max(pos.method_10260() - eye.field_1350, 0), eye.field_1350 - (pos.method_10260() + 1));
        return dx * dx + dy * dy + dz * dz;
    }

    private class_243 findVisiblePoint(class_310 client, class_746 player,
                                   class_243 eye, class_2338 pos, double reachSq) {
        for (double gx : GRID) {
            for (double gy : GRID) {
                for (double gz : GRID) {
                    class_243 target = new class_243(pos.method_10263() + gx, pos.method_10264() + gy, pos.method_10260() + gz);
                    if (eye.method_1025(target) > reachSq) continue;
                    class_239 r = client.field_1687.method_17742(new class_3959(
                            eye, target,
                            class_3959.class_3960.field_17558,
                            class_3959.class_242.field_1348,
                            player));
                    if (r.method_17783() == class_239.class_240.field_1333
                            || r.method_17784().method_1025(target) < GRAZE * GRAZE) {
                        return target;
                    }
                }
            }
        }
        return null;
    }

    private void updateForward(class_746 player) {
        class_1297 source = player.method_5765() ? player.method_5854() : player;
        class_243 v = source.method_18798();
        if (v.field_1352 * v.field_1352 + v.field_1350 * v.field_1350 > 1.0E-4) {
            forwardDir = Math.abs(v.field_1352) > Math.abs(v.field_1350)
                    ? (v.field_1352 > 0 ? class_2350.field_11034 : class_2350.field_11039)
                    : (v.field_1350 > 0 ? class_2350.field_11035 : class_2350.field_11043);
        } else if (forwardDir == null) {
            forwardDir = player.method_5735();
        }
    }
}
