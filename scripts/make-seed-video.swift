// Builds the tiny bundled test clip that POST /api/v1/dev/seed-feed attaches to its VIDEO post.
//
// Command (from the repo root):
//   swift scripts/make-seed-video.swift server/src/main/resources/dev/seed-video.mp4
//
// Check: `avmediainfo <file>` shows an avc1 video track of about 3 seconds, the file is under
// 64 KB, and the top-level boxes are ftyp, moov, mdat in that order (faststart).
// If a run is interrupted, AVAssetWriter can leave a `seed-video.mp4.sb-*` temp file next to the
// output. Delete it before committing: everything under src/main/resources ships in the server jar.
//
// 30 frames at 10 fps, 320x180, H.264 Baseline, amber (#B08052) with a white bar sweeping
// left to right so playback is visible.

import AVFoundation
import CoreVideo
import Foundation

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data((message + "\n").utf8))
    exit(1)
}

guard CommandLine.arguments.count > 1 else { fail("usage: make-seed-video.swift <output.mp4>") }
let outputUrl = URL(fileURLWithPath: CommandLine.arguments[1])
try? FileManager.default.removeItem(at: outputUrl)

let width = 320
let height = 180
let frameCount = 30
let barWidth = 20

guard let writer = try? AVAssetWriter(outputURL: outputUrl, fileType: .mp4) else { fail("cannot create writer") }
writer.shouldOptimizeForNetworkUse = true

let settings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoProfileLevelKey: AVVideoProfileLevelH264BaselineAutoLevel,
        AVVideoAverageBitRateKey: 100_000,
        AVVideoMaxKeyFrameIntervalKey: 10,
    ],
]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
let adaptor = AVAssetWriterInputPixelBufferAdaptor(
    assetWriterInput: input,
    sourcePixelBufferAttributes: [
        kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
        kCVPixelBufferWidthKey as String: width,
        kCVPixelBufferHeightKey as String: height,
    ]
)
guard writer.canAdd(input) else { fail("cannot add input") }
writer.add(input)
guard writer.startWriting() else { fail("startWriting failed: \(String(describing: writer.error))") }
writer.startSession(atSourceTime: .zero)

func makeBuffer(frame: Int) -> CVPixelBuffer? {
    var buffer: CVPixelBuffer?
    CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32BGRA, nil, &buffer)
    guard let pixels = buffer else { return nil }
    CVPixelBufferLockBaseAddress(pixels, [])
    defer { CVPixelBufferUnlockBaseAddress(pixels, []) }
    guard let base = CVPixelBufferGetBaseAddress(pixels) else { return nil }
    let rowBytes = CVPixelBufferGetBytesPerRow(pixels)
    let barStart = frame * (width - barWidth) / (frameCount - 1)
    for y in 0..<height {
        let row = base.advanced(by: y * rowBytes).assumingMemoryBound(to: UInt8.self)
        for x in 0..<width {
            let inBar = x >= barStart && x < barStart + barWidth
            let offset = x * 4
            row[offset] = inBar ? 0xFF : 0x52     // B
            row[offset + 1] = inBar ? 0xFF : 0x80 // G
            row[offset + 2] = inBar ? 0xFF : 0xB0 // R
            row[offset + 3] = 0xFF                // A
        }
    }
    return pixels
}

for frame in 0..<frameCount {
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.005) }
    guard let pixels = makeBuffer(frame: frame) else { fail("cannot build frame \(frame)") }
    guard adaptor.append(pixels, withPresentationTime: CMTime(value: CMTimeValue(frame), timescale: 10)) else {
        fail("append failed at frame \(frame): \(String(describing: writer.error))")
    }
}
input.markAsFinished()
let done = DispatchSemaphore(value: 0)
writer.finishWriting { done.signal() }
done.wait()
guard writer.status == .completed else { fail("finishWriting failed: \(String(describing: writer.error))") }
