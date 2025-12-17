layout(std140) uniform TimeData {
    float u_timeOfDay;           // 0.0 to 1.0
    float u_rawTimeOfDay;        // unbent time
    float u_time;                // elapsed time in seconds
    float u_randomNoiseFromDay;  // daily noise seed
    float u_deltaTime;           // frame delta
    int u_currentHour;
    int u_currentMinute;
    int u_currentDay;
};