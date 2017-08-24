package com.alibaba.sdk.android;

import android.content.Context;
import android.os.Environment;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.OSSConstants;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSCustomSignerCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.common.utils.IOUtils;
import com.alibaba.sdk.android.oss.common.utils.OSSUtils;
import com.alibaba.sdk.android.oss.model.AppendObjectRequest;
import com.alibaba.sdk.android.oss.model.AppendObjectResult;
import com.alibaba.sdk.android.oss.model.DeleteBucketRequest;
import com.alibaba.sdk.android.oss.model.DeleteBucketResult;
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest;
import com.alibaba.sdk.android.oss.model.DeleteObjectResult;
import com.alibaba.sdk.android.oss.model.GetBucketACLRequest;
import com.alibaba.sdk.android.oss.model.GetBucketACLResult;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.ListObjectsRequest;
import com.alibaba.sdk.android.oss.model.ListObjectsResult;
import com.alibaba.sdk.android.oss.model.ListPartsRequest;
import com.alibaba.sdk.android.oss.model.ListPartsResult;
import com.alibaba.sdk.android.oss.model.CreateBucketRequest;
import com.alibaba.sdk.android.oss.model.CreateBucketResult;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest;
import com.alibaba.sdk.android.oss.model.ResumableUploadResult;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by zhouzhuo on 11/24/15.
 */
public class OSSTestConfig {

    public static final String ENDPOINT = "http://oss-cn-beijing.aliyuncs.com";

    public static final String EXCLUDE_HOST = "oss-cn-beijing.aliyuncs.com";

    public static final String ANDROID_TEST_BUCKET = "king-soft";

    public static final String PUBLIC_READ_BUCKET = "public-read-android";

    public static final String ANDROID_TEST_CNAME = "http://king-soft.chenhongyu.cn/";

    public static final String ANDROID_TEST_LOCATION = "oss-cn-beijing";

    public static final String FOR_LISTOBJECT_BUCKET = "constant-listobject-test";

    public static final String PUBLIC_READ_WRITE_BUCKET = "public-read-write-android";

    public static final String CREATE_TEMP_BUCKET = "test-create-bucket-xyc";

//    uploadFilePath = Environment.getExternalStorageDirectory()
//            .getAbsolutePath() + File.separator + DIR_NAME + File.separator + FILE_NAME;

    public static final String FILE_DIR = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator+"oss/";

    public static final String TOKEN_URL = "http://localhost:8080/distribute-token.json";

    public static final String CALLBACK_SERVER  = "callback.oss-demo.com:23450";

    public static final String AK = "LTAI6j5oGTpwbiUU";

    public static final String SK = "YnGrZ5cqqGphoUwljeCHceij8Uotzf";

    public static OSSCredentialProvider credentialProvider;
    public static OSSCredentialProvider fadercredentialProvider;
    public static OSSCredentialProvider fadercredentialProviderWrong;
    public static OSSCredentialProvider plainTextAKSKcredentialProvider = newPlainTextAKSKCredentialProvider();
    private static OSSTestConfig sInstance;

    private OSSTestConfig(Context context){
        credentialProvider = newStsTokenCredentialProvider(context);
        fadercredentialProvider = newFederationCredentialProvider(context);
        fadercredentialProviderWrong = newFederationCredentialProviderWrongExpiration(context);
    }

    public static OSSTestConfig instance(Context context){
        if(sInstance == null) {
            sInstance = new OSSTestConfig(context.getApplicationContext());
        }
        return sInstance;
    }

    public static OSSCredentialProvider newCustomSignerCredentialProvider() {
        return new OSSCustomSignerCredentialProvider() {
            @Override
            public String signContent(String content) {
                return OSSUtils.sign(AK, SK, content);
            }
        };
    }

    public static OSSCredentialProvider newStsTokenCredentialProvider(Context context) {
        try {
            InputStream input = context.getAssets().open("sts.json");
            String jsonText = IOUtils.readStreamAsString(input, OSSConstants.DEFAULT_CHARSET_NAME);
            JSONObject jsonObjs = new JSONObject(jsonText);
            String ak = jsonObjs.getString("AccessKeyId");
            String sk = jsonObjs.getString("AccessKeySecret");
            String token = jsonObjs.getString("SecurityToken");
            OSSStsTokenCredentialProvider ossStsTokenCredentialProvider = new OSSStsTokenCredentialProvider(ak, sk, token);
            OSSLog.logD("[ak] "+ossStsTokenCredentialProvider.getAccessKeyId(),false);
            OSSLog.logD("[sk] "+ossStsTokenCredentialProvider.getSecretKeyId(),false);
            OSSLog.logD("[token] "+ossStsTokenCredentialProvider.getSecurityToken(),false);
            return ossStsTokenCredentialProvider;
        } catch (Exception e) {
            OSSLog.logE(e.toString());
            e.printStackTrace();
            return new OSSStsTokenCredentialProvider("", "", "");
        }
    }

    public static OSSCredentialProvider newFederationCredentialProvider(final Context context) {
        return new OSSFederationCredentialProvider() {
            @Override
            public OSSFederationToken getFederationToken() {
                OSSLog.logE("[getFederationToken] -------------------- ");
                try {
                    InputStream input = context.getAssets().open("sts.json");
                    String jsonText = IOUtils.readStreamAsString(input, OSSConstants.DEFAULT_CHARSET_NAME);
                    JSONObject jsonObjs = new JSONObject(jsonText);
                    String ak = jsonObjs.getString("AccessKeyId");
                    String sk = jsonObjs.getString("AccessKeySecret");
                    String token = jsonObjs.getString("SecurityToken");
                    String expiration = jsonObjs.getString("Expiration");
                    return new OSSFederationToken(ak, sk, token, expiration);
                } catch (Exception e) {
                    OSSLog.logE(e.toString());
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    public static OSSCredentialProvider newFederationCredentialProviderWrongExpiration(final Context context) {
        return new OSSFederationCredentialProvider() {
            @Override
            public OSSFederationToken getFederationToken() {
                OSSLog.logE("[getFederationToken] -------------------- ");
                try {
                    InputStream input = context.getAssets().open("sts.json");
                    String jsonText = IOUtils.readStreamAsString(input, OSSConstants.DEFAULT_CHARSET_NAME);
                    JSONObject jsonObjs = new JSONObject(jsonText);
                    String ak = jsonObjs.getString("AccessKeyId");
                    String sk = jsonObjs.getString("AccessKeySecret");
                    String token = jsonObjs.getString("SecurityToken");
                    String expiration = jsonObjs.getString("WrongExpiration");
                    return new OSSFederationToken(ak, sk, token, expiration);
                } catch (Exception e) {
                    OSSLog.logE(e.toString());
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    public static OSSCredentialProvider newPlainTextAKSKCredentialProvider() {
        OSSPlainTextAKSKCredentialProvider provider = new OSSPlainTextAKSKCredentialProvider(AK, SK);
        OSSLog.logD("[ak] "+provider.getAccessKeyId(),false);
        OSSLog.logD("[sk] "+provider.getAccessKeySecret(),false);
        return provider;
    }

    public final static class TestDeleteCallback implements OSSCompletedCallback<DeleteObjectRequest, DeleteObjectResult> {

        public DeleteObjectRequest request;
        public DeleteObjectResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(DeleteObjectRequest request, DeleteObjectResult result) {
            this.request = request;
            this.result = result;
        }

        @Override
        public void onFailure(DeleteObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestGetCallback implements OSSCompletedCallback<GetObjectRequest, GetObjectResult> {

        public GetObjectRequest request;
        public GetObjectResult result;
        public ClientException clientException;
        public ServiceException serviceException;


        @Override
        public void onSuccess(GetObjectRequest request, GetObjectResult result) {
            this.request = request;
            this.result = result;
        }

        @Override
        public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestPutCallback implements OSSCompletedCallback<PutObjectRequest, PutObjectResult> {

        public PutObjectRequest request;
        public PutObjectResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(PutObjectRequest request, PutObjectResult result) {
            this.request = request;
            this.result = result;
        }

        @Override
        public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestResumableUploadCallback implements OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult> {

        public ResumableUploadRequest request;
        public ResumableUploadResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(ResumableUploadRequest request, ResumableUploadResult result) {
            this.request = request;
            this.result = result;
        }

        @Override
        public void onFailure(ResumableUploadRequest request, ClientException clientExcepion, ServiceException serviceException) {
            if (clientExcepion != null) {
                clientExcepion.printStackTrace();
            }
            if (serviceException != null) {
                serviceException.printStackTrace();
            }
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestAppendCallback implements OSSCompletedCallback<AppendObjectRequest, AppendObjectResult> {

        public AppendObjectRequest request;
        public AppendObjectResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(AppendObjectRequest request, AppendObjectResult result) {
            this.request = request;
            this.result = result;
            OSSLog.logD("ObjectCRC64: "+result.getObjectCRC64());
            OSSLog.logD("NextPosition: "+result.getNextPosition());
        }

        @Override
        public void onFailure(AppendObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestListObjectsCallback implements OSSCompletedCallback<ListObjectsRequest, ListObjectsResult> {

        public ListObjectsRequest request;
        public ListObjectsResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(ListObjectsRequest request, ListObjectsResult result) {
            this.request = request;
            this.result = result;
        }

        @Override
        public void onFailure(ListObjectsRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }


    public final static class TestCreateBucketCallback implements OSSCompletedCallback<CreateBucketRequest, CreateBucketResult> {

        public CreateBucketRequest request;
        public CreateBucketResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(CreateBucketRequest request, CreateBucketResult result) {
            this.request = request;
            this.result = result;
            OSSLog.logV("[Location]="+result.bucketLocation);
        }

        @Override
        public void onFailure(CreateBucketRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestDeleteBucketCallback implements OSSCompletedCallback<DeleteBucketRequest, DeleteBucketResult> {

        public DeleteBucketRequest request;
        public DeleteBucketResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(DeleteBucketRequest request, DeleteBucketResult result) {
            this.request = request;
            this.result = result;
        }

        @Override
        public void onFailure(DeleteBucketRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }

    public final static class TestGetBucketACLCallback implements OSSCompletedCallback<GetBucketACLRequest, GetBucketACLResult> {

        public GetBucketACLRequest request;
        public GetBucketACLResult result;
        public ClientException clientException;
        public ServiceException serviceException;

        @Override
        public void onSuccess(GetBucketACLRequest request, GetBucketACLResult result) {
            this.request = request;
            this.result = result;
            OSSLog.logD("BucketOwner "+result.getBucketOwner(),false);
            OSSLog.logD("BucketOwnerID "+result.getBucketOwnerID(),false);
        }

        @Override
        public void onFailure(GetBucketACLRequest request, ClientException clientExcepion, ServiceException serviceException) {
            this.request = request;
            this.clientException = clientExcepion;
            this.serviceException = serviceException;
        }
    }
}
