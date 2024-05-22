package com.amazonaws.services.msf.pojo;

public class Rules {
        private String rule;
        private String key;

        private String id;

        // Empty constructor (you can modify this as needed)
        public Rules() {
        }

        // Constructor with fields
        public Rules(String rule, String key, String id) {
            this.rule = rule;
            this.key = key;
            this.id = id;
        }

        // Getter and Setter methods for rule
        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

        // Getter and Setter methods for key
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        // toString method for easy debugging
        @Override
        public String toString() {
            return "Rules{" +
                    "rule='" + rule + '\'' +
                    ", key='" + key + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }
    }
