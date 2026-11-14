package application.kernel.profilerpipeline.profilermanager;

import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class ProfilerGLSLUtility extends EngineUtility {

    /*
     * Stateless timestamp query helpers for GpuTimerBranch. Every call runs
     * against the GL context current at the time, which must be the context
     * that owns the queries. Package-private.
     */

    // Queries \\

    static int[] generateQueries(int count) {

        int[] queries = new int[count];

        for (int i = 0; i < count; i++)
            queries[i] = EngineContext.gl40.glGenQuery();

        return queries;
    }

    static void deleteQueries(int[] queries) {
        for (int i = 0; i < queries.length; i++)
            EngineContext.gl40.glDeleteQuery(queries[i]);
    }

    // Timestamps \\

    static void stampTimestamp(int query) {
        EngineContext.gl40.glQueryCounter(query, EngineSetting.GL_TIMESTAMP);
    }

    static boolean isResultAvailable(int query) {
        return EngineContext.gl40.glGetQueryObjecti(query, EngineSetting.GL_QUERY_RESULT_AVAILABLE) != 0;
    }

    static long readTimestamp(int query) {
        return EngineContext.gl40.glGetQueryObjecti64(query, EngineSetting.GL_QUERY_RESULT);
    }
}
