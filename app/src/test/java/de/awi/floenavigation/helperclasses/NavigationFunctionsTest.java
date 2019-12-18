package de.awi.floenavigation.helperclasses;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NavigationFunctionsTest {

    @Test
    public void calculateNewPosition() {
    }

    @Test
    public void calculateDifference1() {
        double expected = 1851.31378905 * 60 * 10;
        double actual = NavigationFunctions.calculateDifference(0,0,0,10);
        assertEquals(expected, actual, 0.0001);
    }
    @Test
    public void calculateDifference2() {
        double expected = 1851.31378905 * 60 * 10;
        double actual = NavigationFunctions.calculateDifference(0,0,10,0);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    public void calculateBearing() {
    }

    @Test
    public void convertToDegMinSec() {
    }

    @Test
    public void calculateAngleBeta1() {
        double expectedBeta = 0;
        double actualBeta = NavigationFunctions.calculateAngleBeta(0, 0, 0, 10);
        assertEquals(expectedBeta, actualBeta, 0.0001);
    }

    @Test
    public void calculateAngleBeta2() {
        double expectedBeta = 90;
        double actualBeta = NavigationFunctions.calculateAngleBeta(0, 0, 10, 0);
        assertEquals(expectedBeta, actualBeta, 0.0001);
    }

    @Test
    public void calculateAngleBeta3() {
        double expectedBeta = 180;
        double actualBeta = NavigationFunctions.calculateAngleBeta(0, 0, 0, -10);
        assertEquals(expectedBeta, actualBeta, 0.0001);
    }

    @Test
    public void calculateAngleBeta4() {
        double expectedBeta = 270;
        double actualBeta = NavigationFunctions.calculateAngleBeta(0, 0, -10, 0);
        assertEquals(expectedBeta, actualBeta, 0.0001);
    }

    @Test
    public void calculateAngleBeta5() {
        double expectedBeta = 45;
        double actualBeta = NavigationFunctions.calculateAngleBeta(80, 80, 85, 85);
        assertEquals(expectedBeta, actualBeta, 0.0001);
    }

    @Test
    public void locationInDegrees() {
    }
}