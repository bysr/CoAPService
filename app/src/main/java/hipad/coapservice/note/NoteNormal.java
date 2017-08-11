package hipad.coapservice.note;


public class NoteNormal implements INote {
    String noteName;

    public NoteNormal(String noteName) {
        this.noteName = noteName;
    }

    @Override
    public String getNoteName() {
        return noteName;
    }
}
