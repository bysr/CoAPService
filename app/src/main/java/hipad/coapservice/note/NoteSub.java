package hipad.coapservice.note;

/**
 * Created by wangyawen on 2017/8/11 0011.
 */

public class NoteSub implements INote {
    String noteName;

    public NoteSub(String noteName) {
        this.noteName = noteName;
    }

    @Override
    public String getNoteName() {
        return noteName;
    }
}
