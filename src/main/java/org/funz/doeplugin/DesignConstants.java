package org.funz.doeplugin;

public interface DesignConstants {

    /**
     * Used in Status as return code.
     */
    public enum Decision {
        // Design is ready next iteration
        READY_FOR_NEXT_ITERATION,
        /// There is no more experiences to carry out for current project.
        DESIGN_OVER,
        /// Design requires a confirmation or additional information from user.
        ASK_USER,
        /// An error is occured. Design is to be stopped.
        ERROR,
        NONE;
    }
    public static String NODESIGNER_ID = "No design of experiments";

    /**
     * Returned aech time the designer is asked for next iteration. Message must
     * be non null for decisions ASK_USER and ERROR.
     */
    public static class Status {

        public Status(Decision d, String msg) {
            _decision = d;
            _message = msg;
        }

        public Status(Decision d) {
            _decision = d;
        }

        public Decision getDecision() {
            return _decision;
        }

        public void set(Status s) {
            _decision = s.getDecision();
            _message = s.getMessage();
        }

        public String getMessage() {
            return _message;
        }
        private Decision _decision;
        private String _message;

        @Override
        public String toString() {
            return _decision + " " + _message;
        }
    }
}
