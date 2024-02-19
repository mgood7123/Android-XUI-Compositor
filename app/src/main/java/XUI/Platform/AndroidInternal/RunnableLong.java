package XUI.Platform.AndroidInternal;

@FunctionalInterface
public interface RunnableLong {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void run(long t);
}
