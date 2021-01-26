//
// Created by arch on 1/24/21.
//

#ifndef DETECT_NATIVE_H
#define DETECT_NATIVE_H
#include "net.h"

namespace yolocv {
    typedef struct {
        int width;
        int height;
    } YoloSize;
}

typedef struct {
    std::string name;
    int stride;
    std::vector<yolocv::YoloSize> anchors;
} YoloLayerData;

typedef struct BoxInfo {
    float x1;
    float y1;
    float x2;
    float y2;
    float score;
    int label;
} BoxInfo;
#endif //DETECT_NATIVE_H
