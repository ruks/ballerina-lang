/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ballerinalang.net.grpc.nativeimpl.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.grpc.GrpcConstants;
import org.ballerinalang.net.grpc.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ballerinalang.bre.bvm.BLangVMErrors.STRUCT_GENERIC_ERROR;
import static org.ballerinalang.net.grpc.GrpcConstants.ORG_NAME;
import static org.ballerinalang.net.grpc.GrpcConstants.REQUEST_SENDER;
import static org.ballerinalang.util.BLangConstants.BALLERINA_BUILTIN_PKG;

/**
 * Native function to send server error the caller.
 *
 * @since 1.0.0
 */
@BallerinaFunction(
        orgName = ORG_NAME,
        packageName = GrpcConstants.PROTOCOL_PACKAGE_GRPC,
        functionName = "sendError",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = GrpcConstants.GRPC_CLIENT,
                structPackage = GrpcConstants.PROTOCOL_STRUCT_PACKAGE_GRPC),
        args = {@Argument(name = "statusCode", type = TypeKind.INT),
                @Argument(name = "message", type = TypeKind.STRING)},
        returnType = @ReturnType(type = TypeKind.RECORD, structType = STRUCT_GENERIC_ERROR, structPackage =
                BALLERINA_BUILTIN_PKG),
        isPublic = true
)
public class SendError extends BlockingNativeCallableUnit {
    private static final Logger LOG = LoggerFactory.getLogger(SendError.class);

    @Override
    public void execute(Context context) {
        BStruct connectionStruct = (BStruct) context.getRefArgument(0);
        long statusCode = context.getIntArgument(0);
        String errorMsg = context.getStringArgument(0);

        StreamObserver requestSender = (StreamObserver) connectionStruct.getNativeData(REQUEST_SENDER);
        if (requestSender == null) {
            context.setError(MessageUtils.getConnectorError(context, new StatusRuntimeException(Status
                    .fromCode(Status.INTERNAL.getCode()).withDescription("Error while sending the error. Response" +
                            " observer not found."))));
        } else {
            try {
                requestSender.onError(new StatusRuntimeException(Status.fromCodeValue((int) statusCode).withDescription
                        (errorMsg)));
            } catch (Throwable e) {
                LOG.error("Error while sending error to server.", e);
                context.setError(MessageUtils.getConnectorError(context, e));
            }
        }
    }
}
