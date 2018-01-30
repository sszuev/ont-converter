package com.github.sszuev;

/**
 * Not a true test.
 * For manual running only.
 * <p>
 * Created by @szuev on 30.01.2018.
 */
public class TestExamples {

    public static class SimpleTest {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\out -o out-2 -of 0 -v -f";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleTestPunnings {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\out -o out-1 -of 0 -v -f -p 2";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleTestMN {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\out -o out-3 -of 12 -v -f";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleTestForce {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\out -o out-f -of 13 -v";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleSpin1 {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\src\\test\\resources\\etc\\spif.ttl -if 0 -o out\\spif.omn -of 12 -s -v -r -w";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleSpin2 {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\src\\test\\resources\\etc\\spinmap.spin.ttl -if 0 -o out\\spinmap.tex -of 21 -s -v -r -w";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleSpin3 {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\src\\test\\resources\\etc\\spinmapl.spin.ttl -if 0 -o out\\spinmapl.fss -of 13 -s -v -r -w";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class SimpleTestRefine {
        public static void main(String... args) throws Exception {
            String cmd = "-i ..\\..\\ont-api\\src\\test\\resources -o out-3 -of 12 -v -f -r";
            Main.main(cmd.split("\\s+"));
        }
    }

    public static class HelpPrint {
        public static void main(String... args) throws Exception {
            Main.main("-h");
        }
    }
}
