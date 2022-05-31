package com.example.webrtcservermodulee;

import static android.service.controls.ControlsProviderService.TAG;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.sip.SipManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Capturer;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
    List<PeerConnection.IceServer> mIceServers = new ArrayList<>();
    List<PeerConnection.IceServer> mIceServers2 = new ArrayList<>();
    Button button, button2, button3;
    Thread mThread;
    private static Intent mediaProjectionResultIntent;
    private static int mMediaProjectionPermissionResultCode;
    JSONObject offerObject;
    Surface mSurface;
    EglBase.Context eglBaseContext;
    org.webrtc.SurfaceViewRenderer  remoteView;
    ArrayList<String> listOfStunServers = new ArrayList<>();
    private PeerConnectionFactory mPeerConnectionFactory;
    MediaProjectionManager mediaProjectionManager;
    PeerConnection.Observer mObserver;
    MediaStream localMediaStream;
    private PeerConnection mPeerConnection;
    private DataChannel mDataChannel;
    SessionDescription mSessionDescription;
    private int mResultCode;
    MediaProjectionManager manager;
    VideoCapturer mVideoCapturer;
    private MediaProjection mediaProjection;
    private MediaProjection.Callback mCallback;
    SurfaceTextureHelper surfaceTextureHelper;
    VideoSource mVideoSource;
    VideoTrack localVideoTrack;
//    private SessionDescription mSessionDescription;
    IMyAidlInterface myAidlInterface;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.e(TAG, "onCreate: Some log");
        eglBaseContext=EglBase.create().getEglBaseContext();
        requestPermissions(new String[]{Manifest.permission.CAMERA},420);

        ServiceConnection serviceConnection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                myAidlInterface= IMyAidlInterface.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        remoteView = findViewById(R.id.remoteView);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        initSurfaceView(remoteView);

        /*mediaProjection = manager.getMediaProjection(m, data);
        mediaProjection.stop();*/
//        initializePeerConnectionFactory();
        for (String stunServer :
                listOfStunServers) {
//            mIceServers.add(createIceServers(stunServer));
        }


//        try {
//            myAidlInterface.addNumber(20,50);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
        mIceServers2.add(createIceServers("stun4.l.google.com:19302"));
        for (PeerConnection.IceServer mIceServer :
                mIceServers) {
            Log.e(TAG, "onCreate: Ice server = " + mIceServer);
        }
        initializePeerConnectionFactory();
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocalIP_Address();
                mThread = new Thread(new ServerStartThread());
                mThread.start();
            }
        });

        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataChannel.Init init = new DataChannel.Init();
                DataChannel dataChannel = mPeerConnection.createDataChannel("dc001", init);
                ByteBuffer mBuffer = stringToByteBuffer(" Hey client. Rohit implemented webrtc datachannel");
                DataChannel.Buffer mBuffer2 = new DataChannel.Buffer(mBuffer, false);
                dataChannel.send(mBuffer2);
                Log.e(TAG, "onClick: " + dataChannel.label());
                Log.e(TAG, "onClick: " + dataChannel.state());
                Log.e(TAG, "onClick: " + dataChannel.toString());
//                SipManager m=getSystemService(SipManager.);
            }
        });

        button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.e(TAG, "onClick: Stream added " );
//                setUpVideoData();
                mPeerConnection.addStream(localMediaStream);

            }
        });
    }



    public void getLocalIP_Address() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo mWifiInfo = wifiManager.getConnectionInfo();
        int ipInt = mWifiInfo.getIpAddress();

//        mWifiInfo.get
        try {
            Log.e(TAG, "getLocalIP_Address: " + InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(
                            ByteOrder.LITTLE_ENDIAN
                    ).putInt(ipInt).array()).getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    private VideoCapturer createScreenCapturer() {
        if (mMediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            Log.e(TAG, "createScreenCapturer: User didn't give permission to capture the screen");
            return null;
        }
        return new ScreenCapturerAndroid(
                mediaProjectionResultIntent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.e(TAG, "onStop: \n" +
                        "                User revoked permission to capture the screen.");
            }
        });
    }

    public ServerSocket mServerSocket;
    public String SERVER_IP;
    public static final int SERVER_PORT = 8080;
    private PrintWriter mPrintWriter;
    private BufferedReader mBufferedReader;

    private class ServerStartThread implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                mServerSocket = new ServerSocket(SERVER_PORT);
                Log.e(TAG, "run: Server socket created but not connected yet");
                socket = mServerSocket.accept();
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                mBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "run: Connected to " + socket.getRemoteSocketAddress());

                mThread = new Thread(new ReadMessageFromClient());
                mThread.start();

                manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadMessageFromClient implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            Log.e(TAG, "run: Read Message Thread started");

            while (true) {
                try {
                    final String message = mBufferedReader.readLine();
                    Log.e(TAG, "run: Inside while loop");
                    if (message != null) {
                        Log.e(TAG, "run: CLIENT: " + message);
                        JSONObject mObject = new JSONObject(message);

                        Log.e(VideoFrameDrawer.TAG, "run: " + mObject.getString("operation"));
//                        Log.e(VideoFrameDrawer.TAG, "run: "+mObject.getString("type") );
//                        Log.e(VideoFrameDrawer.TAG, "run: "+mObject.getString("description") );

                        String operation = mObject.getString("operation");
                        if (operation.equalsIgnoreCase("ANSWER_SDP")) {
                            SessionDescription remoteSDP = new SessionDescription(SessionDescription.Type.ANSWER, mObject.getString("description"));
                            setRemoteSessionDescription(remoteSDP);
                        } else if (operation.equalsIgnoreCase("IceCandidate")) {
                            IceCandidate mIceCandidate = new IceCandidate(
                                    mObject.getString("sdpMid"),
                                    mObject.getInt("sdpMLineIndex"),
                                    mObject.getString("sdp")
                            );
                            mPeerConnection.addIceCandidate(mIceCandidate);
                            Log.e(VideoFrameDrawer.TAG, "run: ICE CANDIDATE ADDED ");

                        }
                    } else {
                        Log.e(TAG, "run: Message is null");
                        mThread = new Thread(new ServerStartThread());
                        mThread.start();
                        return;
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "run: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

            }
        }
    }

    private class SendMessageToClient implements Runnable {
        private Object message;

        public SendMessageToClient(Object message) {
            this.message = message;
        }

        @Override
        public void run() {
            mPrintWriter.println(message);
            mPrintWriter.flush();
            Log.e(TAG, "run: Message sent to client: " + message);
        }
    }

    public void initSurfaceView(org.webrtc.SurfaceViewRenderer surfaceViewRenderer) {
        surfaceViewRenderer.init(eglBaseContext, null);
        surfaceViewRenderer.setEnableHardwareScaler(true);
    }

    public void createPeerConnection() {

        List<PeerConnection.IceServer> mIceServers = new ArrayList<>();

        PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302");
        iceServerBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
        PeerConnection.IceServer iceServer = iceServerBuilder.createIceServer();
        mIceServers.add(iceServer);
        PeerConnection.IceServer.Builder iceServerBuilder2 = PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302");
        iceServerBuilder2.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
        PeerConnection.IceServer iceServer2 = iceServerBuilder2.createIceServer();
        mIceServers.add(iceServer2);

        MediaConstraints constraints = new MediaConstraints();
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

//        AudioSource audioSource=mPeerConnectionFactory.createAudioSource(new MediaConstraints());
//        VideoSource localVideoSource = mPeerConnectionFactory.createVideoSource(true);
//        AudioTrack localAudioTrack=mPeerConnectionFactory.createAudioTrack("_audioTrack",audioSource);
//        VideoTrack localVideoTrack = mPeerConnectionFactory.createVideoTrack("_videoTrack", localVideoSource);
//        localVideoTrack.addSink(remoteView);
        mObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.e(TAG, "onSignalingChange: " + signalingState.toString());
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.e(TAG, "onIceConnectionChange: " + iceConnectionState.name());
                if(iceConnectionState== PeerConnection.IceConnectionState.DISCONNECTED){
                    createOffer();
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.e(TAG, "onIceConnectionReceivingChange: " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.e(TAG, "onIceGatheringChange: " + iceGatheringState.name());
                if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                    createOffer();
                }
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
//                Log.e(TAG, "onIceCandidate: "+iceCandidate.sdp);
                Log.e(VideoFrameDrawer.TAG, "onIceCandidate: SDP =  " + iceCandidate.sdp);
//                Log.e(VideoFrameDrawer.TAG, "onIceCandidate: Server URL =  " + iceCandidate.serverUrl);
//                Log.e(VideoFrameDrawer.TAG, "onIceCandidate: SDP Mid =  " + iceCandidate.sdpMid);
//                Log.e(VideoFrameDrawer.TAG, "onIceCandidate: Adapter Type =  " + iceCandidate.adapterType);
//                Log.e(VideoFrameDrawer.TAG, "onIceCandidate: sdpMLineIndex =  " + iceCandidate.sdpMLineIndex);

                mPeerConnection.addIceCandidate(iceCandidate);
                JSONObject mObject = new JSONObject();
                try {
                    mObject.put("operation", "IceCandidate");
                    mObject.put("sdp", iceCandidate.sdp);
                    mObject.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                    mObject.put("sdpMid", iceCandidate.sdpMid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mThread = new Thread(new SendMessageToClient(mObject.toString()));
                mThread.start();

            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.e(TAG, "onIceCandidatesRemoved: Remaining ICE candidates= " + Arrays.toString(iceCandidates));
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.e(TAG, "onAddStream: " + mediaStream.getId());
//                VideoSink remote_view = null;
//                mediaStream.videoTracks.get(0).
                VideoTrack mVideoTrack=mediaStream.videoTracks.get(0);
                mVideoTrack.setEnabled(true);
                mVideoTrack.addSink(remoteView);

                createOffer();
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.e(TAG, "onRemoveStream: " + mediaStream.getId());
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.e(TAG, "onDataChannel: " + dataChannel.label());
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
                        Log.e(TAG, "onBufferedAmountChange: " + l);
                    }

                    @Override
                    public void onStateChange() {
                        Log.e(TAG, "onStateChange: "+dataChannel.state());
                    }

                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        String msg = byteBufferToString(buffer.data);
                        Log.e(TAG, "onMessage: Received a new message =>" + msg);
                        JSONObject mObject = null;
                        String operation = null;

                        try {
                            mObject = new JSONObject(msg);
                            operation = mObject.getString("operation");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(operation.equalsIgnoreCase("fling")){
                            Fling fling=Fling.getInstance();
                            int x1= 0,x2=0,y1=0,y2=0;
                            try {
                                x1 = mObject.getInt("x1");
                                x2=mObject.getInt("x2");
                                y1=mObject.getInt("y1");
                                y2=mObject.getInt("y2");
                                Log.e(TAG, "onMessage: Received values= "+x1+" "+y1+" "+x2+" "+y2);

                                fling.simulateGesture(x1,y1,x2,y2,500);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }else if(operation.equalsIgnoreCase("tap")){
                            Fling fling=Fling.getInstance();
                            try {
                                int x1=mObject.getInt("x1");
                                int y1=mObject.getInt("y1");
                                fling.simulateGesture(x1,y1,null,null,500);
                            }catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else if(operation.equalsIgnoreCase("recent")){
                            Fling fling=Fling.getInstance();
                            fling.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                        }else if(operation.equalsIgnoreCase("home")){
                            Fling fling=Fling.getInstance();
                            fling.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                        }else if(operation.equalsIgnoreCase("back")){
                            Fling fling=Fling.getInstance();
                            fling.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        }
                    }
                });
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.e(TAG, "onRenegotiationNeeded: ");
                createOffer();
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.e(TAG, "onAddTrack: " + rtpReceiver.id() + " " + Arrays.toString(mediaStreams));
            }
        };

        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(mIceServers);

        mPeerConnection = mPeerConnectionFactory.createPeerConnection(rtcConfiguration, mObserver);
//        mPeerConnection.addStream(localMediaStream);
        Log.e(TAG, "createPeerConnection: DONE 2");

//        mPeerConnection.addTrack(localAudioTrack);
//        mPeerConnection.addTrack(localVideoTrack);
// I NEED A SIGNALLING SERVER FOR EXCHANGING ICE CANDIDATES
//        createAnswer();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE) {
            mediaProjectionResultIntent = data;
            mMediaProjectionPermissionResultCode = resultCode;
            createPeerConnection();
            setUpVideoData();
            if(!isAccessibilityServiceGranted()){
                Intent intent=new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent,0);
            }
        }
    }

    private void setUpVideoData() {
//        mVideoCapturer = new ScreenCapturerAndroid(mediaProjectionResultIntent, new MediaProjection.Callback() {
//            @Override
//            public void onStop() {
//                super.onStop();
//                Log.e(TAG, "onStop: Video capturer stopped");
//            }
//        });
        EglBase.Context eglBaseContext = EglBase.create().getEglBaseContext();
        Camera2Enumerator camera2Enumerator=new Camera2Enumerator(getApplicationContext());
        String[] deviceNames= camera2Enumerator.getDeviceNames();
        for (String deviceName:
             deviceNames) {
            Log.e(TAG, "setUpVideoData: Camera Device Name = "+deviceName );
        }
        mVideoCapturer=new Camera2Capturer(MainActivity.this, "0", new CameraVideoCapturer.CameraEventsHandler() {
            @Override
            public void onCameraError(String s) {
                Log.e(TAG, "onCameraError: Camera error callback "+s );
            }

            @Override
            public void onCameraDisconnected() {
                Log.e(TAG, "onCameraDisconnected: Camera Disconnected" );
            }

            @Override
            public void onCameraFreezed(String s) {
                Log.e(TAG, "onCameraFreezed: Camera Frozen "+s);
            }

            @Override
            public void onCameraOpening(String s) {
                Log.e(TAG, "onCameraOpening: "+s);
            }

            @Override
            public void onFirstFrameAvailable() {
                Log.e(TAG, "onFirstFrameAvailable: ");
            }

            @Override
            public void onCameraClosed() {
                Log.e(TAG, "onCameraClosed: ");
            }
        });

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        localMediaStream= mPeerConnectionFactory.createLocalMediaStream("Rohit_Server_Video_Stream");
        mVideoSource = mPeerConnectionFactory.createVideoSource(false);
        DisplayMetrics metrics = new DisplayMetrics();
        MainActivity.this.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        mVideoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), mVideoSource.getCapturerObserver());
        if (mVideoCapturer != null) {
            mVideoCapturer.startCapture(metrics.widthPixels,metrics.heightPixels,60);
        }
        localVideoTrack = mPeerConnectionFactory.createVideoTrack("ARDAMSv0", mVideoSource);
        localVideoTrack.setEnabled(true);
        localMediaStream.addTrack(localVideoTrack);
    }

    public void sendToDataChannel(String m) {
        ByteBuffer data = stringToByteBuffer(m);
        DataChannel.Init init = new DataChannel.Init();
        mDataChannel = mPeerConnection.createDataChannel("DataChannel", init);
        DataChannel.Buffer mBuffer = new DataChannel.Buffer(data, false);
        mDataChannel.send(mBuffer);
    }

    public PeerConnection.IceServer createIceServers(String uri) {
        PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder(uri);
        iceServerBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
        return iceServerBuilder.createIceServer();
    }

    public void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions
                        .builder(getApplicationContext())
                        .setEnableInternalTracer(true)
                        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableNetworkMonitor = true;
//        options.disableEncryption=true;
        mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBaseContext))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    private void setLocalSessionDescription(SessionDescription sessionDescription) {
//        RtpSender
        mPeerConnection.setLocalDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.e(TAG, "onCreateSuccess: Local SDP created");
            }

            @Override
            public void onSetSuccess() {
                Log.e(TAG, "onSetSuccess: Local SDP set");
                offerObject = new JSONObject();
                try {
                    offerObject.put("operation", "OFFER_SDP");
                    offerObject.put("type", mSessionDescription.type);
                    offerObject.put("description", mSessionDescription.description);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mThread = new Thread(new SendMessageToClient(offerObject.toString()));
                mThread.start();

            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure: Local SDP creation failure");

            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetFailure: Local SDP set failure");

            }
        }, sessionDescription);

    }

    private void setRemoteSessionDescription(SessionDescription sdp) {
        mPeerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.e(TAG, "onCreateSuccess: Remote SDP created successfully ");
            }

            @Override
            public void onSetSuccess() {
                Log.e(TAG, "onSetSuccess: Remote SDP set successfully");
//                sendToDataChannel("I live in India");
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure: ");
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetFailure: Remote SDP set failure ");
                createOffer();
            }
        }, sdp);

    }

    private void createOffer() {
        MediaConstraints constraints = new MediaConstraints();
        mPeerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                setLocalSessionDescription(sessionDescription);
                Log.e(TAG, "onCreateSuccess: Offer SDP created successfully");
                mSessionDescription = sessionDescription;
//                offerObject=new JSONObject();
//                try {
//                    offerObject.put("type",sessionDescription.type);
//                    offerObject.put("description",sessionDescription.description);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                mThread=new Thread(new SendMessageToClient(offerObject.toString()));
//                mThread.start();

            }

            @Override
            public void onSetSuccess() {
                Log.e(TAG, "onCreateSuccess: Offer SDP set successfully");
//                offerObject=new JSONObject();
//                try {
//                    offerObject.put("type",mSessionDescription.type);
//                    offerObject.put("description",mSessionDescription.description);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                mThread=new Thread(new SendMessageToClient(offerObject.toString()));
//                mThread.start();

            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateSuccess: Offer SDP creation failure");
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onCreateSuccess: Offer SDP set failure");
            }
        }, constraints);
    }


    public static ByteBuffer stringToByteBuffer(String msg) {
        return ByteBuffer.wrap(msg.getBytes(Charset.defaultCharset()));
    }

    public static String byteBufferToString(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return new String(bytes, Charset.defaultCharset());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPeerConnection != null) {
            mPeerConnection.dispose();
        }

        if (mVideoCapturer != null) {
            mVideoCapturer.dispose();
        }

        if (mVideoSource != null) {
            mVideoSource.dispose();
        }

        if (mPeerConnectionFactory != null) {
            mPeerConnectionFactory.dispose();
        }

        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();

        if (mediaProjection != null) {
            mediaProjection.stop();
        }

//        localView.release();
        remoteView.release();

    }

    public boolean isAccessibilityServiceGranted(){
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/com.example.webrtcservermodulee.Fling";

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}

