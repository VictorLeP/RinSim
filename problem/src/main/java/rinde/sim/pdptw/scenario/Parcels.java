package rinde.sim.pdptw.scenario;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.scenario.Locations.LocationGenerator;
import rinde.sim.pdptw.scenario.TimeSeries.TimeSeriesGenerator;
import rinde.sim.pdptw.scenario.TimeWindows.TimeWindowGenerator;
import rinde.sim.pdptw.scenario.TimeWindows.TravelModel;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.SupplierRngs;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

public final class Parcels {

  public static Builder builder() {
    return new Builder();
  }

  public interface ParcelGenerator {
    ImmutableList<AddParcelEvent> generate(long seed,
        TravelModel travelModel, long endTime);

    // TODO should these location methods be here?
    Point getCenter();

    Point getMin();

    Point getMax();
  }

  static class DefaultParcelGenerator implements ParcelGenerator {
    private final RandomGenerator rng;
    private final TimeSeriesGenerator arrivalTimeGenerator;
    private final LocationGenerator locationGenerator;
    private final TimeWindowGenerator timeWindowGenerator;
    private final SupplierRng<Long> pickupDurationGenerator;
    private final SupplierRng<Long> deliveryDurationGenerator;
    private final SupplierRng<Integer> neededCapacityGenerator;

    DefaultParcelGenerator(Builder b) {
      rng = new MersenneTwister();
      arrivalTimeGenerator = b.arrivalTimeGenerator;
      locationGenerator = b.locationGenerator;
      timeWindowGenerator = b.timeWindowGenerator;
      pickupDurationGenerator = b.pickupDurationGenerator;
      deliveryDurationGenerator = b.deliveryDurationGenerator;
      neededCapacityGenerator = b.neededCapacityGenerator;
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed,
        TravelModel travelModel, long endTime) {
      rng.setSeed(seed);
      final ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList
          .builder();
      final List<Double> times = arrivalTimeGenerator.generate(rng.nextLong());
      final Iterator<Point> locs = locationGenerator.generate(rng.nextLong(),
          times.size() * 2).iterator();

      for (final double time : times) {
        final long arrivalTime = DoubleMath.roundToLong(time,
            RoundingMode.HALF_DOWN);
        final Point origin = locs.next();
        final Point destination = locs.next();

        final ParcelDTO.Builder parcelBuilder = ParcelDTO
            .builder(origin, destination)
            .arrivalTime(arrivalTime)
            .pickupDuration(pickupDurationGenerator.get(rng.nextLong()))
            .deliveryDuration(deliveryDurationGenerator.get(rng.nextLong()))
            .neededCapacity(neededCapacityGenerator.get(rng.nextLong()));

        timeWindowGenerator.generate(rng.nextLong(), parcelBuilder,
            travelModel, endTime);

        eventList.add(new AddParcelEvent(parcelBuilder.build()));
      }
      return eventList.build();
    }

    @Override
    public Point getCenter() {
      return locationGenerator.getCenter();
    }

    @Override
    public Point getMin() {
      return locationGenerator.getMin();
    }

    @Override
    public Point getMax() {
      return locationGenerator.getMax();
    }
  }

  public static class Builder {
    static final TimeSeriesGenerator DEFAULT_ARRIVAL_TIMES = TimeSeries
        .homogenousPoisson(4 * 60 * 60 * 1000, 20);
    static final LocationGenerator DEFAULT_LOCATIONS = Locations.builder()
        .square(5d).uniform();

    static final SupplierRng<Long> DEFAULT_SERVICE_DURATION = SupplierRngs
        .constant(5 * 60 * 1000L);
    static final SupplierRng<Integer> DEFAULT_CAPACITY = SupplierRngs
        .constant(0);

    TimeSeriesGenerator arrivalTimeGenerator;
    TimeWindowGenerator timeWindowGenerator;
    LocationGenerator locationGenerator;
    SupplierRng<Long> pickupDurationGenerator;
    SupplierRng<Long> deliveryDurationGenerator;
    SupplierRng<Integer> neededCapacityGenerator;

    Builder() {
      arrivalTimeGenerator = DEFAULT_ARRIVAL_TIMES;
      locationGenerator = DEFAULT_LOCATIONS;
      pickupDurationGenerator = DEFAULT_SERVICE_DURATION;
      deliveryDurationGenerator = DEFAULT_SERVICE_DURATION;
      neededCapacityGenerator = DEFAULT_CAPACITY;

    }

    public Builder arrivalTimes(TimeSeriesGenerator atg) {
      arrivalTimeGenerator = atg;
      return this;
    }

    public Builder timeWindows(TimeWindowGenerator twg) {
      timeWindowGenerator = twg;
      return this;
    }

    public Builder locations(LocationGenerator lg) {
      locationGenerator = lg;
      return this;
    }

    public Builder pickupDurations(SupplierRng<Long> durations) {
      pickupDurationGenerator = durations;
      return this;
    }

    public Builder deliveryDurations(SupplierRng<Long> durations) {
      deliveryDurationGenerator = durations;
      return this;
    }

    public Builder serviceDurations(SupplierRng<Long> durations) {
      return pickupDurations(durations).deliveryDurations(durations);
    }

    public Builder neededCapacities(SupplierRng<Integer> capacities) {
      neededCapacityGenerator = capacities;
      return this;
    }

    public ParcelGenerator build() {
      return new DefaultParcelGenerator(this);
    }
  }
}
