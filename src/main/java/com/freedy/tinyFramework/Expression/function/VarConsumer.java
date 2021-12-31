package com.freedy.tinyFramework.Expression.function;

/**
 * @author Freedy
 * @date 2021/12/24 17:14
 */
@SuppressWarnings("unchecked")
public interface VarConsumer extends Functional {

    @FunctionalInterface
    interface _1ParameterConsumer<A> extends VarConsumer {
        @VarargsFunction
        void accept(A... param1) throws Exception;
    }

    @FunctionalInterface
    interface _2ParameterConsumer<A, B> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B... param2) throws Exception;
    }

    @FunctionalInterface
    interface _3ParameterConsumer<A, B, C> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C... param3) throws Exception;
    }

    @FunctionalInterface
    interface _4ParameterConsumer<A, B, C, D> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C param3, D... param4) throws Exception;
    }

    @FunctionalInterface
    interface _5ParameterConsumer<A, B, C, D, E> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C param3, D param4, E... param5) throws Exception;
    }

    @FunctionalInterface
    interface _6ParameterConsumer<A, B, C, D, E, F> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C param3, D param4, E param5, F... param6) throws Exception;
    }

    @FunctionalInterface
    interface _7ParameterConsumer<A, B, C, D, E, F, G> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C param3, D param4, E param5, F param6, G... param7) throws Exception;
    }

    @FunctionalInterface
    interface _9ParameterConsumer<A, B, C, D, E, F, G> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C param3, D param4, E param5, F param6, G param7, G... param8) throws Exception;
    }

    @FunctionalInterface
    interface _10ParameterConsumer<A, B, C, D, E, F, G, H> extends VarConsumer {
        @VarargsFunction
        void accept(A param1, B param2, C param3, D param4, E param5, F param6, G param7, G param8, H... param9) throws Exception;
    }
}
