/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;

/**
 * Collection of usefule comparators
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ComparatorFactory {

    private ComparatorFactory() {
    }

    public static class EnrichmentResultByNESComparator implements Comparator {

        Order fOrder;

        public EnrichmentResultByNESComparator(final Order order) {
            this.fOrder = order;
        }

        public int compare(final Object pn1, final Object pn2) {

            if ((pn1 == null) && (pn2 == null)) {
                return 0;     // cant compare
            } else if (pn1 == null) {
                return fOrder.isAscending() ? -1 : +1;    // null is always least
            } else if (pn2 == null) {
                return fOrder.isAscending() ? +1 : -1;    // null is always least
            }

            final EnrichmentResult result1 = (EnrichmentResult) pn1;
            final EnrichmentResult result2 = (EnrichmentResult) pn2;

            final float nes1 = result1.getScore().getNES();
            final float nes2 = result2.getScore().getNES();

            if (Float.isNaN(nes1) && Float.isNaN(nes2)) {
                return 0;
            } else if (Float.isNaN(nes1)) {
                return fOrder.isAscending() ? -1 : +1;
            } else if (Float.isNaN(nes2)) {
                return fOrder.isAscending() ? +1 : -1;
            }


            if (fOrder.isAscending()) {
                if (nes1 < nes2) {
                    return -1;
                } else if (nes1 > nes2) {
                    return +1;
                } else {
                    return 0;
                }

            } else {
                if (nes1 < nes2) {
                    return +1;
                } else if (nes1 > nes2) {
                    return -1;
                } else {
                    return 0;
                }
            }

        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    }    // End class EnrichmentResultComparator


    public static class PobComparator implements Comparator {

        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
         */
        public int compare(Object pn1, Object pn2) {

            PersistentObject pob1 = (PersistentObject) pn1;
            PersistentObject pob2 = (PersistentObject) pn2;
            return pob1.getName().compareTo(pob2.getName());
        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    }    // End FileExtComparator

    public static class FileExtComparator implements Comparator {

        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
         */
        public int compare(Object pn1, Object pn2) {

            String ext1 = FilenameUtils.getExtension(pn1.toString());
            String ext2 = FilenameUtils.getExtension(pn2.toString());

            return ext1.compareTo(ext2);
        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    }    // End FileExtComparator

    public static class ScoredDatasetScoreComparator implements Comparator<String> {
        final ScoredDataset fSds;

        public ScoredDatasetScoreComparator(ScoredDataset sds) {
            this.fSds = sds;
        }

        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
         */
        public int compare(String pn1, String pn2) {

            int rank1 = fSds.getRank(pn1);
            int rank2 = fSds.getRank(pn2);

            if (rank1 == -1 || rank2 == -1) {
                throw new IllegalArgumentException("Specified label not in the sds: " + pn1 + " " + pn2);
            }

            return rank1 - rank2;
        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    }

    public static Comparator<PersistentObject> PERSISTENT_OBJECT_BY_NAME = new Comparator<PersistentObject>() {
        @Override
        public int compare(PersistentObject o1, PersistentObject o2) {
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.getName().compareTo(o2.getName());
        }
    };
    
    public static class ChipNameComparator implements Comparator<String> {

        /* private static member used to track highest seen version */
        private static String highestVersionId;
        private static DefaultArtifactVersion highestVersion;

        public static String getHighestVersionId() {
            return highestVersionId;
        }

        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
         */
        public int compare(String s1, String s2) {
            
            String versionId1 = s1.substring(s1.lastIndexOf("v"), s1.lastIndexOf(".chip"));
            String versionId2 = s2.substring(s2.lastIndexOf("v"), s2.lastIndexOf(".chip"));
            
            DefaultArtifactVersion version1 = new DefaultArtifactVersion(versionId1);
            DefaultArtifactVersion version2 = new DefaultArtifactVersion(versionId2);

            /* want to keep track of highest version seen */
            if (highestVersion == null) {
                if (version1.compareTo(version2) < 0) {
                    highestVersion = version2;
                    highestVersionId = versionId2;
                }
                else {
                    highestVersion = version1;
                    highestVersionId = versionId1;
                }
            }
            else {
                if (highestVersion.compareTo(version2) < 0) {
                    highestVersion = version2;
                    highestVersionId = versionId2;
                }
            }

            if (!version1.equals(version2)) {
                return version2.compareTo(version1);
            }

            // now just string comparison
            return s1.compareTo(s2);
        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    }    // End ChipNameComparator
}