/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uni.julio.supertvplus.view.exoplayer;

import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;


 public class EventLogger implements Player.EventListener,
          AnalyticsListener, AdaptiveMediaSourceEventListener,
          ExtractorMediaSource.EventListener, DefaultDrmSessionManager.EventListener,
          MetadataRenderer.Output {

    private static final String TAG = "EventLogger";
    private static final int MAX_TIMELINE_ITEM_LINES = 3;
    private static final NumberFormat TIME_FORMAT;
    static {
      TIME_FORMAT = NumberFormat.getInstance(Locale.US);
      TIME_FORMAT.setMinimumFractionDigits(2);
      TIME_FORMAT.setMaximumFractionDigits(2);
      TIME_FORMAT.setGroupingUsed(false);
    }

    private final MappingTrackSelector trackSelector;
    private final Timeline.Window window;
    private final Timeline.Period period;
    private final long startTimeMs;

    public EventLogger(MappingTrackSelector trackSelector) {
      this.trackSelector = trackSelector;
      window = new Timeline.Window();
      period = new Timeline.Period();
      startTimeMs = SystemClock.elapsedRealtime();
    }



    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
      // ;//Log.d(TAG, "state [" + getSessionTimeString() + ", " + playWhenReady + ", "
      // + getStateString(state) + "]");
    }



    @Override
    public void onTimelineChanged(EventTime eventTime,@Player.TimelineChangeReason int reason) {
      if (eventTime.timeline == null) {
        return;
      }
      int periodCount = eventTime.timeline.getPeriodCount();
      int windowCount = eventTime.timeline.getWindowCount();
      ;//Log.d(TAG, "sourceInfo [periodCount=" + periodCount + ", windowCount=" + windowCount);
      for (int i = 0; i < Math.min(periodCount, MAX_TIMELINE_ITEM_LINES); i++) {
        eventTime.timeline.getPeriod(i, period);
        ;//Log.d(TAG, "  " +  "period [" + getTimeString(period.getDurationMs()) + "]");
      }
      if (periodCount > MAX_TIMELINE_ITEM_LINES) {
        ;//Log.d(TAG, "  ...");
      }
      for (int i = 0; i < Math.min(windowCount, MAX_TIMELINE_ITEM_LINES); i++) {
        eventTime.timeline.getWindow(i, window);
        ;//Log.d(TAG, "  " +  "window [" + getTimeString(window.getDurationMs()) + ", "
        //+ window.isSeekable + ", " + window.isDynamic + "]");
      }
      if (windowCount > MAX_TIMELINE_ITEM_LINES) {
        ;//Log.d(TAG, "  ...");
      }
      ;//Log.d(TAG, "]");
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
      ;//Log.e(TAG, "playerFailed [" + getSessionTimeString() + "]", e);
    }

    @Override
    public void onTracksChanged(TrackGroupArray ignored, TrackSelectionArray trackSelections) {
      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo == null) {
        ;//Log.d(TAG, "Tracks []");
        return;
      }
      ;//Log.d(TAG, "Tracks [");
      // Log tracks associated to renderers.
      for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.length; rendererIndex++) {
        TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
        TrackSelection trackSelection = trackSelections.get(rendererIndex);
        if (rendererTrackGroups.length > 0) {
          ;//Log.d(TAG, "  Renderer:" + rendererIndex + " [");
          for (int groupIndex = 0; groupIndex < rendererTrackGroups.length; groupIndex++) {
            TrackGroup trackGroup = rendererTrackGroups.get(groupIndex);
            String adaptiveSupport = getAdaptiveSupportString(trackGroup.length,
                    mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false));
            ;//Log.d(TAG, "    Group:" + groupIndex + ", adaptive_supported=" + adaptiveSupport + " [");
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
              String status = getTrackStatusString(trackSelection, trackGroup, trackIndex);
              String formatSupport = getFormatSupportString(
                      mappedTrackInfo.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex));
              // ;//Log.d(TAG, "      " + status + " Track:" + trackIndex + ", "
              // + getFormatString(trackGroup.getFormat(trackIndex))
              // + ", supported=" + formatSupport);
            }
            ;//Log.d(TAG, "    ]");
          }
          // Log metadata for at most one of the tracks selected for the renderer.
          if (trackSelection != null) {
            for (int selectionIndex = 0; selectionIndex < trackSelection.length(); selectionIndex++) {
              Metadata metadata = trackSelection.getFormat(selectionIndex).metadata;
              if (metadata != null) {
                ;//Log.d(TAG, "    Metadata [");
                printMetadata(metadata, "      ");
                ;//Log.d(TAG, "    ]");
                break;
              }
            }
          }
          ;//Log.d(TAG, "  ]");
        }
      }
      // Log tracks not associated with a renderer.
      TrackGroupArray unassociatedTrackGroups = mappedTrackInfo.getUnassociatedTrackGroups();
      if (unassociatedTrackGroups.length > 0) {
        ;//Log.d(TAG, "  Renderer:None [");
        for (int groupIndex = 0; groupIndex < unassociatedTrackGroups.length; groupIndex++) {
          ;//Log.d(TAG, "    Group:" + groupIndex + " [");
          TrackGroup trackGroup = unassociatedTrackGroups.get(groupIndex);
          for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
            String status = getTrackStatusString(false);
            String formatSupport = getFormatSupportString(
                    RendererCapabilities.FORMAT_UNSUPPORTED_TYPE);
            // ;//Log.d(TAG, "      " + status + " Track:" + trackIndex + ", "
            // + getFormatString(trackGroup.getFormat(trackIndex))
            // + ", supported=" + formatSupport);
          }
          ;//Log.d(TAG, "    ]");
        }
        ;//Log.d(TAG, "  ]");
      }
      ;//Log.d(TAG, "]");
    }

    // MetadataRenderer.Output




    // StreamingDrmSessionManager.EventListener

    @Override
    public void onDrmSessionManagerError(Exception e) {
      printInternalError("drmSessionManagerError", e);
    }

    @Override
    public void onDrmKeysRestored() {

    }

    @Override
    public void onDrmKeysRemoved() {

    }

    @Override
    public void onDrmKeysLoaded() {
      ;//Log.d(TAG, "drmKeysLoaded [" + getSessionTimeString() + "]");
    }

    // ExtractorMediaSource.EventListener

    @Override
    public void onLoadError(IOException error) {
      printInternalError("loadError", error);
    }


    // Internal methods

    private void printInternalError(String type, Exception e) {
      ;//Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e);
    }

    private void printMetadata(Metadata metadata, String prefix) {
      for (int i = 0; i < metadata.length(); i++) {
        Metadata.Entry entry = metadata.get(i);
      }
    }




    private static String getFormatSupportString(int formatSupport) {
      switch (formatSupport) {
        case RendererCapabilities.FORMAT_HANDLED:
          return "YES";
        case RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES:
          return "NO_EXCEEDS_CAPABILITIES";
        case RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE:
          return "NO_UNSUPPORTED_TYPE";
        case RendererCapabilities.FORMAT_UNSUPPORTED_TYPE:
          return "NO";
        default:
          return "?";
      }
    }

    private static String getAdaptiveSupportString(int trackCount, int adaptiveSupport) {
      if (trackCount < 2) {
        return "N/A";
      }
      switch (adaptiveSupport) {
        case RendererCapabilities.ADAPTIVE_SEAMLESS:
          return "YES";
        case RendererCapabilities.ADAPTIVE_NOT_SEAMLESS:
          return "YES_NOT_SEAMLESS";
        case RendererCapabilities.ADAPTIVE_NOT_SUPPORTED:
          return "NO";
        default:
          return "?";
      }
    }

    private static String getFormatString(Format format) {
      if (format == null) {
        return "null";
      }
      StringBuilder builder = new StringBuilder();
      builder.append("id=").append(format.id).append(", mimeType=").append(format.sampleMimeType);
      if (format.bitrate != Format.NO_VALUE) {
        builder.append(", bitrate=").append(format.bitrate);
      }
      if (format.width != Format.NO_VALUE && format.height != Format.NO_VALUE) {
        builder.append(", res=").append(format.width).append("x").append(format.height);
      }
      if (format.frameRate != Format.NO_VALUE) {
        builder.append(", fps=").append(format.frameRate);
      }
      if (format.channelCount != Format.NO_VALUE) {
        builder.append(", channels=").append(format.channelCount);
      }
      if (format.sampleRate != Format.NO_VALUE) {
        builder.append(", sample_rate=").append(format.sampleRate);
      }
      if (format.language != null) {
        builder.append(", language=").append(format.language);
      }
      return builder.toString();
    }

    private static String getTrackStatusString(TrackSelection selection, TrackGroup group,
                                               int trackIndex) {
      return getTrackStatusString(selection != null && selection.getTrackGroup() == group
              && selection.indexOf(trackIndex) != C.INDEX_UNSET);
    }

    private static String getTrackStatusString(boolean enabled) {
      return enabled ? "[X]" : "[ ]";
    }

    @Override
    public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

    }

    @Override
    public void onMediaPeriodReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

    }

    @Override
    public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {

    }

    @Override
    public void onReadingStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

    }

    @Override
    public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {

    }

   @Override
   public void onMetadata(Metadata metadata) {

   }
 }
