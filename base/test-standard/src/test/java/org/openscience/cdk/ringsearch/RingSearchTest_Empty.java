/*
 * Copyright (C) 2012 John May <jwmay@users.sf.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. All we ask is that proper credit is given for our
 * work, which includes - but is not limited to - adding the above copyright
 * notice to the beginning of your source code files, and to any copyright
 * notice that you may distribute with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.ringsearch;

import org.junit.Test;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * ring search unit tests for an empty molecule
 *
 * @author John May
 * @cdk.module test-standard
 */
public final class RingSearchTest_Empty {

    private final IAtomContainer empty = new AtomContainer(0, 0, 0, 0);

    @Test
    public void testCyclic() {
        assertThat(new RingSearch(empty).cyclic().length, is(0));
    }

    @Test
    public void testCyclic_Int() {
        int n = empty.getAtomCount();
        RingSearch ringSearch = new RingSearch(empty);
        for (int i = 0; i < n; i++) {
            assertFalse(ringSearch.cyclic(i));
        }
    }

    @Test
    public void testIsolated() {
        assertThat(new RingSearch(empty).isolated().length, is(0));
    }

    @Test
    public void testFused() {
        assertThat(new RingSearch(empty).fused().length, is(0));
    }

    @Test
    public void testRingFragments() {
        assertTrue(new RingSearch(empty).ringFragments().isEmpty());
    }

    @Test
    public void testIsolatedRingFragments() {
        assertTrue(new RingSearch(empty).isolatedRingFragments().isEmpty());
    }

    @Test
    public void testFusedRingFragments() {
        assertTrue(new RingSearch(empty).fusedRingFragments().isEmpty());
    }

}
