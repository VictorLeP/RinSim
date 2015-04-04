/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator.SimulatorEventType;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.base.Suppliers;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 *
 */
public class SimulatorTest {

  @SuppressWarnings("null")
  private Simulator simulator;

  @Before
  public void setUp() {
    simulator = Simulator.builder()
      .setRandomGenerator(new MersenneTwister(123L))
      .setTickLength(100L)
      .setTimeUnit(SI.SECOND)
      .build();
    TestUtil.testEnum(SimulatorEventType.class);
  }

  /**
   * Models should not register.
   */
  @SuppressWarnings("deprecation")
  @Test(expected = UnsupportedOperationException.class)
  public void testRegisterModel() {
    simulator.register(new DummyModel());
  }

  /**
   * Models should not register.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRegisterModel2() {
    simulator.register((Object) new DummyModel());
  }

  @Test
  public void testRegister() {
    final DummyModel m1 = new DummyModel();
    final DummyModel m2 = new DummyModel();
    final DummyModelAsTickListener m3 = new DummyModelAsTickListener();
    final Simulator sim = Simulator.builder()
      .addModel(Suppliers.ofInstance(m1))
      .addModel(Suppliers.ofInstance(m2))
      .addModel(Suppliers.ofInstance(m3))
      .build();

    assertThat((Iterable<?>) sim.getModels()).containsAllOf(m1, m2, m3)
      .inOrder();

    sim.register(new DummyObject());

    final DummyObjectTickListener dotl = new DummyObjectTickListener();
    sim.register(dotl);

    final DummyObjectSimulationUser dosu = new DummyObjectSimulationUser();
    sim.register(dosu);
    assertEquals(sim, dosu.getAPI());

    sim.unregister(new DummyObject());
    sim.unregister(new DummyObjectTickListener());

  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnregisterModel() {
    simulator.unregister(new DummyModel());
  }

  @Test
  public void testStartWithoutConfiguring() {
    final LimitingTickListener ltl = new LimitingTickListener(simulator, 3);
    simulator.addTickListener(ltl);
    simulator.start();
    assertEquals(3000, simulator.getCurrentTime());
  }

  @Test
  public void testGetRnd() {
    assertNotNull(simulator.getRandomGenerator());
  }

  class DummyObject {}

  class DummyObjectTickListener implements TickListener {
    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}
  }

  class DummyObjectSimulationUser implements SimulatorUser {
    private SimulatorAPI receivedAPI;

    @Override
    public void setSimulator(SimulatorAPI api) {
      receivedAPI = api;
    }

    public SimulatorAPI getAPI() {
      return receivedAPI;
    }
  }

  class DummyModelAsTickListener extends DummyModel implements TickListener {

    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}

  }

  class LimitingTickListener implements TickListener {
    private final int limit;
    private int tickCount;
    private final Simulator sim;

    public LimitingTickListener(Simulator s, int tickLimit) {
      sim = s;
      limit = tickLimit;
      tickCount = 0;
    }

    public void reset() {
      tickCount = 0;
    }

    @Override
    public void tick(TimeLapse tl) {
      tickCount++;
    }

    @Override
    public void afterTick(TimeLapse tl) {
      if (tickCount >= limit) {
        assertTrue(sim.isPlaying());
        if (tl.getTime() > limit * tl.getTimeStep()) {
          sim.togglePlayPause();
        }
        sim.stop();
        assertFalse(sim.isPlaying());
        reset();
      }
    }
  }

  class TickListenerImpl implements TickListener {
    private int count = 0;
    private long execTime;
    private long afterTime;

    @Override
    public void tick(TimeLapse tl) {
      count++;
      execTime = System.nanoTime();
    }

    public long getExecTime() {
      return execTime;
    }

    public long getAfterExecTime() {
      return afterTime;
    }

    public int getTickCount() {
      return count;
    }

    @Override
    public void afterTick(TimeLapse tl) {
      afterTime = System.nanoTime();
    }
  }

}
