import React from "react";
import { Composition } from "remotion";
import { Trailer, TRAILER_FRAMES } from "./Trailer";

export const RemotionRoot: React.FC = () => (
  <Composition
    id="Trailer"
    component={Trailer}
    durationInFrames={TRAILER_FRAMES}
    fps={30}
    width={1920}
    height={1080}
  />
);
