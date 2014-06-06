package org.demoth.nogaem.tests.junk;

import org.demoth.nogaem.client.swing.SwingConsole;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.*;

/**
 * @author demoth
 */
public class TestConsole {

    public void test() {
        SwingConsole console = new SwingConsole(System.out::println);
        console.setVisible(true);
        //console.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public static void main(String[] args) {
        SwingConsole console = new SwingConsole(System.out::println);
        console.setVisible(true);
        console.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
}
