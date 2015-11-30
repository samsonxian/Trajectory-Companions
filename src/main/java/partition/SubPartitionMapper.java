package partition;

import geometry.TCLine;
import geometry.TCPoint;
import geometry.TCRegion;
import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SubPartitionMapper implements
        FlatMapFunction<Tuple2<Integer,Iterable<TCPoint>>, Tuple3<Integer, Integer, TCRegion>>,
        Serializable
{
    private int _numSubPartitions = 1;
    private double _epsilon = 0.0;

    public SubPartitionMapper(int numSubpartition, double epsilon)
    {
        _numSubPartitions = numSubpartition;
        _epsilon = epsilon;
    }

    @Override
    public Iterable<Tuple3<Integer, Integer, TCRegion>> call(Tuple2<Integer, Iterable<TCPoint>> slot) throws Exception {

        List<Tuple3<Integer, Integer, TCRegion>> regions = new ArrayList<>();

        double max = getMaxY(slot._2());
        double min = getMinY(slot._2());
        double length = (max - min) / _numSubPartitions;

        for(int i = 1; i <= _numSubPartitions; ++i)
        {
            Tuple3<Integer, Integer, TCRegion> t = new Tuple3<>(slot._1(), new Integer(i), new TCRegion(i, slot._1()));
            regions.add(t);
        }

        int id = 0;
        for (TCPoint point : slot._2())
        {
            id = getSubPartitionId(point, min, length);
            if(id > 0) {
                Tuple3<Integer, Integer, TCRegion> r = regions.get(id - 1);
                r._3().AddPoint(point);
            }
        }

        return regions;
    }

    private double getMaxY(Iterable<TCPoint> points)
    {
        double max = 0.0;
        for (TCPoint point : points)
        {
            if (point.getY() > max)
                max = point.getY();
        }
        return max;
    }

    private double getMinY(Iterable<TCPoint> points)
    {
        double min = 180.0;
        for (TCPoint point : points)
        {
            if (point.getY() < min)
                min = point.getY();
        }
        return min;
    }

    private int getSubPartitionId(TCPoint point, double min, double length)
    {
        double lowerBound = min;
        double upperBound = min + length + _epsilon;
        for(int i = 1; i<= _numSubPartitions; ++i)
        {
            if(point.getY() >= lowerBound && point.getY() <= upperBound)
            {
                return i;
            }
            lowerBound = upperBound - _epsilon;
            upperBound = min + i * length + _epsilon;
        }
        return -1;
    }


}