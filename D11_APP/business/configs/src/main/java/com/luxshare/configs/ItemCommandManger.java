package com.luxshare.configs;

import android.content.Context;
import android.util.Log;

import com.luxshare.configs.bean.ItemCommand;
import com.luxshare.configs.bean.ItemCommand.Command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 用于封装操作指令
 */
final public class ItemCommandManger {
    private static final String TAG = "ItemCommandManger";
    private static ItemCommandManger instance;
    private List<ItemCommand> itemCommands;
    private Context context;

    private ItemCommandManger() {
        init();
    }

    public static ItemCommandManger of() {
        if (instance == null) {
            instance = new ItemCommandManger();
        }
        return instance;
    }

    private void init() {
        context = ContextManager.getInstance().getContext();
        initItemCommands();
    }

    private void initItemCommands() {
        itemCommands = new ArrayList<>();
        //人声定位
//        itemCommands.add(ItemCommand.Builder.news()
//                .setTag(context.getString(R.string.tag_sound_location))
//                .setType(Configs.TYPE_SINGLE_CHOICE)
//                .setNicky(Configs.CMD_LOCATION)
//                .addCommand(new Command(Configs.KEY_SOUND_ON, Configs.OPERATE_SWITCH_ON))
//                .addCommand(new Command(Configs.KEY_SOUND_OFF, Configs.OPERATE_SWITCH_OFF))
//                .build());
        //自动回连
//        itemCommands.add(ItemCommand.Builder.news()
//                .setTag(context.getString(R.string.tag_reconnect))
//                .setType(Configs.TYPE_SWITCH)
//                .setNicky(Configs.CMD_RECONNECT)
//                .addCommand(new Command(Configs.KEY_RECONNECT_ON, Configs.OPERATE_SWITCH_ON))
//                .addCommand(new Command(Configs.KEY_RECONNECT_OFF, Configs.OPERATE_SWITCH_OFF))
//                .build());
        //语音提醒
//        itemCommands.add(ItemCommand.Builder.news()
//                .setTag(context.getString(R.string.tag_remind))
//                .setType(Configs.TYPE_SINGLE_CHOICE)
//                .setNicky(Configs.CMD_REMIND)
//                .addCommand(new Command(Configs.KEY_REMIND_ON, Configs.OPERATE_SWITCH_ON))
//                .addCommand(new Command(Configs.KEY_REMIND_OFF, Configs.OPERATE_SWITCH_OFF))
//                .build());
        //find my开关状态
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_find_my))
                .setType(Configs.TYPE_SWITCH)
                .setNicky(Configs.CMD_FIND_MY)
                .addCommand(new Command(Configs.KEY_REMIND_ON, Configs.OPERATE_SWITCH_ON))
                .addCommand(new Command(Configs.KEY_REMIND_OFF, Configs.OPERATE_SWITCH_OFF))
                .build());
        //语音提醒语言
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_language))
                .setType(Configs.TYPE_SINGLE_CHOICE)
                .setNicky(Configs.CMD_LANGUAGE)
                .addCommand(new Command(Configs.KEY_LANGUAGE_CN,  Configs.OPERATE_LANGUAGE_CN))
                .addCommand(new Command(Configs.KEY_LANGUAGE_EN,  Configs.OPERATE_LANGUAGE_EN))
                .build());
        //自动关机
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_lauto_off))
                .setType(Configs.TYPE_SINGLE_CHOICE)
                .setNicky(Configs.CMD_AUTO_SHUTDOWN)
                .addCommand(new Command(Configs.KEY_TIME_TEN, Configs.OPERATE_TIME_TEN))
                .addCommand(new Command(Configs.KEY_TIME_THIRTY, Configs.OPERATE_TIME_THIRTY))
                .addCommand(new Command(Configs.KEY_TIME_SIXTY, Configs.OPERATE_TIME_SIXTY))
                .addCommand(new Command(Configs.KEY_TIME_ON, Configs.OPERATE_TIME_ON))
                .build());
        //清空蓝牙列表
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_clear_list))
                .setType(Configs.TYPE_ALONE)
                .setNicky(Configs.CMD_CLEAR_LIST)
                .addCommand(new Command(Configs.KEY_CLEAR_PAIR_LIST, Configs.OPERATE_EXE_ONLY))
                .build());
        //恢复出厂设置
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_factory_reset))
                .setType(Configs.TYPE_ALONE)
                .setNicky(Configs.CMD_FACTORY_RESET)
                .addCommand(new Command(Configs.KEY_FACTORY_RESET, Configs.OPERATE_EXE_ONLY))
                .build());
        //一键配网
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_distribution_network))
                .setType(Configs.TYPE_ALONE)
                .setNicky(Configs.CMD_CONFIG_NET_WIFI)
                .build());
        //会议/音乐模式
        itemCommands.add(ItemCommand.Builder.news()
                .setTag(context.getString(R.string.tag_music_mode))
                .setType(Configs.TYPE_SINGLE_CHOICE)
                .setNicky(Configs.CMD_MUSIC_MODE)
                .addCommand(new Command(Configs.KEY_MUSIC_MODE, Configs.OPERATE_MUSIC_MODE))
                .addCommand(new Command(Configs.KEY_MEETING_MODE, Configs.OPERATE_MEETING_MODE))
                .build());
    }


    public List<ItemCommand> getItemCommands() {
        return itemCommands;
    }


    public String getSwitchKeyByState(String tag, boolean state) {
        if (tag.equals(context.getString(R.string.tag_sound_location))) {
            return state ? Configs.KEY_SOUND_ON : Configs.KEY_SOUND_OFF;
        } else if (tag.equals(context.getString(R.string.tag_reconnect))) {
            return state ? Configs.KEY_RECONNECT_ON : Configs.KEY_RECONNECT_OFF;
        } else if (tag.equals(context.getString(R.string.tag_remind))) {
            return state ? Configs.KEY_REMIND_ON : Configs.KEY_REMIND_OFF;
        }
        return "";
    }

    public ItemCommand getItemCommandByTag(String tag) {
        for (ItemCommand itemCommand : itemCommands) {
            if (tag.equals(itemCommand.getTag())) {
                return itemCommand;
            }
        }
        return null;
    }

    public int getCommandBy(ItemCommand itemCommand, String key) {
        List<Command> commands = itemCommand.getCommands();
        for (Command c : commands) {
            if (key.equals(c.getName())) {
                return c.getValue();
            }
        }
        return Configs.OPERATE_INVALID;
    }

    public static void printByteArray(String tag, byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        builder.append("bytes:");
        for (int i = 0; i < bytes.length; i++) {
            if (i < bytes.length - 1) {
                builder.append(Integer.toHexString(bytes[i]) + ",");
            } else {
                builder.append(Integer.toHexString(bytes[i]));
            }
        }
        Log.i(tag, "printByteArray: " + builder);
    }

    public byte[] getSendCmd(byte modleId, int operate) {
        return new byte[]{Configs.CMD_SEND_HEAD, 5
                , Configs.CMD_GROUP_SETTING, modleId, (byte) operate};
    }

    public byte[] getConfigNetCmd(byte modleId, String wifiName, String pwd) {
        byte[] wifiNameBytes = wifiName.getBytes(StandardCharsets.UTF_8);
        byte[] pwdBytes = pwd.getBytes(StandardCharsets.UTF_8);
        Log.i(TAG, "getSendCmd: wifiNameBytes.length:" + wifiNameBytes.length
                + ",pwdBytes.length:" + pwdBytes.length);
        int num = 5 + wifiNameBytes.length + pwdBytes.length;
        byte[] head = {Configs.CMD_SEND_HEAD, (byte) num
                , Configs.CMD_GROUP_SETTING, modleId, (byte) wifiNameBytes.length};
        byte[] result = new byte[head.length + wifiNameBytes.length + pwdBytes.length];
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(wifiNameBytes, 0, result, head.length, wifiNameBytes.length);
        System.arraycopy(pwdBytes, 0, result, head.length + wifiNameBytes.length, pwdBytes.length);
        ItemCommandManger.of().printByteArray(TAG, result);
        return result;
    }

    /**
     * 获取查询命令
     *
     * @return
     */
    public byte[] getqueryCmd() {
        return new byte[]{Configs.CMD_SEND_HEAD, 4
                , Configs.CMD_GROUP_SETTING, Configs.CMD_QUERY};
    }
}
