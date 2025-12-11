package com.luxshare.configs.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装每个item所要操作的指令
 */
public class ItemCommand {
    /**
     * View的tag
     */
    private String tag;
    /**
     * 昵称指令
     */
    private byte nicky;

    /**
     * 1、一种操作；
     * 2、开关操作；
     * 3、多种单选操作；
     */
    private int type;

    /**
     * 指令
     */
    private List<Command> commands;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public byte getNicky() {
        return nicky;
    }

    public void setNicky(byte nicky) {
        this.nicky = nicky;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }


    /**
     * 指令key,value;
     */
    public static class Command {
        private String name;
        private int value;

        public Command(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /**
     * 构建类
     */
    public static class Builder {
        private String tag;
        private int type;
        private byte nicky;
        private List<Command> commands = new ArrayList<>();

        private Builder() {
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public byte getNicky() {
            return nicky;
        }

        public Builder setNicky(byte nicky) {
            this.nicky = nicky;
            return this;
        }

        public Builder addCommand(Command command) {
            this.commands.add(command);
            return this;
        }

        public static Builder news() {
            return new Builder();
        }

        public ItemCommand build() {
            ItemCommand itemCommand = new ItemCommand();
            itemCommand.setTag(this.tag);
            itemCommand.setType(this.type);
            itemCommand.setNicky(this.nicky);
            itemCommand.setCommands(this.commands);
            return itemCommand;
        }
    }
}
