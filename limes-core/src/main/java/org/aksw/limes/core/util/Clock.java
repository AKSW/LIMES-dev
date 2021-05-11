/*
 * LIMES Core Library - LIMES – Link Discovery Framework for Metric Spaces.
 * Copyright © 2011 Data Science Group (DICE) (ngonga@uni-paderborn.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aksw.limes.core.util;

/**
 *
 * @author ngonga
 */
public class Clock {
    long begin;
    long lastClick;

    public Clock() {
        begin = System.currentTimeMillis();
        lastClick = begin;
    }

    public long durationSinceClick() {
        long help = lastClick;
        lastClick = System.currentTimeMillis();
        return lastClick - help;
    }

    public long totalDuration() {
        lastClick = System.currentTimeMillis();
        return lastClick - begin;
    }
}
