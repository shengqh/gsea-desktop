/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;

import java.util.*;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DatasetMetrics {

    /**
     * Class Constructor.
     */
    public DatasetMetrics() {
    }

    /**
     * Score AND sort/order a Dataset according to specified parameters
     * <p/>
     * Also see the Unsorted version below for better perf in some
     * cases.
     *
     * @param metric
     * @param sort
     * @param order
     * @param ds
     * @param template
     * @return
     */
    public ScoredDataset scoreDataset(final Metric metric,
                                      final SortMode sort,
                                      final Order order,
                                      final Map metricParams,
                                      final LabelledVectorProcessor lvp,
                                      final Dataset ds,
                                      final Template template) {

        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }
        
        // DONT check for Template -- it can be null for some metrics
        if (sort == null) {
            throw new IllegalArgumentException("Param sort cannot be null");
        }
        
        if (order == null) {
            throw new IllegalArgumentException("Param order cannot be null");
        }
        
        final int rows = ds.getNumRow();
        final DoubleElement[] sorted = new DoubleElement[rows];
                
        for (int i = 0; i < rows; i++) {
            double dist = metric.getScore(ds.getRow(i), template, metricParams);
            final DoubleElement del = new DoubleElement(i, dist);
            sorted[i] = del;
        }
        
        Arrays.parallelSort(sorted, new DoubleElement.DoubleElementComparator(sort, order.isAscending()));
        List<DoubleElement> dels = Arrays.asList(sorted);
        
        lvp.process(dels); // @note
        
        return new ScoredDatasetImpl(new AddressedVector(dels), ds);
    }
}    // End DatasetMetrics
