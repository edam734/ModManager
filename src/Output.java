public class Output {
    private final String operation;
    private final int nRemoved;
    private final int nSkipped;
    private final int nCopied;


    public Output(final String operation, final int nRemoved, final int nSkipped, final int nCopied) {
        this.operation = operation;
        this.nRemoved = nRemoved;
        this.nSkipped = nSkipped;
        this.nCopied = nCopied;
    }

    @Override
    public String toString() {
        return operation + ": " +
                "removed " + nRemoved +
                ", skipped " + nSkipped +
                ", copied " + nCopied;
    }
}
