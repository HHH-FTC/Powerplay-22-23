package org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.OpModes.Auton_Opmodes;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.team8109_Rise.Hardware.Sensors.Camera.OpenCV.VisionPipelines.Signal_Identifier;
import org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.Mechanisms.Chassis;
import org.firstinspires.ftc.team8109_Rise.UserInterface.AutonSelection;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

@Autonomous
public class CycleAuton_RoadRunner extends LinearOpMode {
    OpenCvCamera webcam; //TODO: Improve tracking

    ElapsedTime runtime = new ElapsedTime();
    ElapsedTime globalTime = new ElapsedTime();

    static Signal_Identifier pipeline;

    public enum AutonState {
        GO_TO_SCORE_PRELOAD,
        SCORE_PRELOAD,
        GO_TO_CONE_STACK,
        CYCLE,
        PARK
    }

    public enum CycleState{
        TO_CONE_STACK,
        PICK_UP_CONE,
        TO_HIGH_JUNCTION,
        SCORE_CONE
    }

    Trajectory toJunction;
    Trajectory toConeStack;
    Trajectory parkLeft;
    Trajectory parkMiddle;
    Trajectory parkRight;

    Chassis chassis;
//    ViperSlides slides;


    public AutonState autonState;
    public CycleState cycleState;

    @Override
    public void runOpMode() throws InterruptedException {
        AutonSelection autonSelection = new AutonSelection(gamepad1, telemetry);

        chassis = new Chassis(gamepad1, telemetry, hardwareMap);
//        slides = new ViperSlides(gamepad1, telemetry, hardwareMap);

        Pose2d globalPose = new Pose2d(0,0,0);

        chassis.setPoseEstimate(globalPose);

        toJunction = chassis.trajectoryBuilder(globalPose).splineTo(new Vector2d(57, 12), -Math.toRadians(45)).build();
        toConeStack = chassis.trajectoryBuilder(globalPose).splineTo(new Vector2d(46, -33), -Math.toRadians(90)).build();

        parkLeft = chassis.trajectoryBuilder(globalPose).splineTo(new Vector2d(45, -23), -Math.toRadians(90)).splineTo(new Vector2d(23, -23), -Math.toRadians(180)).build();
        parkMiddle = chassis.trajectoryBuilder(globalPose).splineTo(new Vector2d(45, 0), -Math.toRadians(90)).splineTo(new Vector2d(23, 0), -Math.toRadians(180)).build();
        parkRight = chassis.trajectoryBuilder(globalPose).splineTo(new Vector2d(45, 23), -Math.toRadians(90)).splineTo(new Vector2d(23, 23), -Math.toRadians(180)).build();

        chassis.followTrajectoryAsync(toJunction);
//        WebcamSetup setup = new WebcamSetup(webcam, telemetry, hardwareMap);
//
//        setup.cameraSetup();
        // Set up webcam
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        webcam = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);

        // Set up pipeline
        pipeline = new Signal_Identifier(telemetry);

        webcam.setPipeline(pipeline);

        // Start camera streaming
        webcam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                webcam.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode)
            {
                /*
                 * This will be called if the camera could not be opened
                 */
            }
        });

        autonSelection.setAutonMode();

        while (opModeInInit()){
            if (!pipeline.TargetRect.empty()){
                telemetry.addData("Parking Zone", pipeline.signalPosition);
            }

            telemetry.addLine("Waiting for start");
            telemetry.update();
        }

        pipeline.signalReadingToggle = false;
        pipeline.signalPosition = Signal_Identifier.SignalPosition.LEFT;
        while (opModeIsActive()){

            ChassisControl();
            SlidesControl();
            ArmControl();
            ClawControl();

            telemetry.addData("Currently running", autonSelection.autonSetting);
            telemetry.addData("Parking Zone", pipeline.signalPosition);
            telemetry.update();
        }
    }

    public void ChassisControl(){
        switch (autonState){
            case GO_TO_SCORE_PRELOAD:
                if (!chassis.isBusy()){
                    autonState = AutonState.SCORE_PRELOAD;
                    runtime.reset();
                }
                break;
            case SCORE_PRELOAD:
                //TODO: Take all of this shit out when adding other mechanisms

                if (runtime.seconds() > 3){
                    autonState = AutonState.CYCLE;
                    cycleState = CycleState.TO_CONE_STACK;
                    chassis.followTrajectoryAsync(toConeStack);
                }
                break;
            case CYCLE:
                switch (cycleState){
                    case TO_CONE_STACK:
                        if (!chassis.isBusy()){
                            cycleState = CycleState.PICK_UP_CONE;
                            runtime.reset();
                        }
                        break;
                    case PICK_UP_CONE:
                        if (runtime.seconds() > 3){
                            cycleState = CycleState.TO_HIGH_JUNCTION;
                            chassis.followTrajectoryAsync(toJunction);
                        }
                        break;
                    case TO_HIGH_JUNCTION:
                        if (!chassis.isBusy()){
                            cycleState = CycleState.SCORE_CONE;
                            runtime.reset();
                        }
                        break;
                    case SCORE_CONE:
                        if (runtime.seconds() > 3){
                            cycleState = CycleState.TO_CONE_STACK;
                            chassis.followTrajectoryAsync(toConeStack);
                        }
                        break;
                }

                if (globalTime.seconds() > 25){
                    autonState = AutonState.PARK;
                    switch (pipeline.signalPosition){
                        case LEFT:
                            chassis.followTrajectoryAsync(parkLeft);
                            break;
                        case MIDDLE:
                            chassis.followTrajectoryAsync(parkMiddle);
                            break;
                        case RIGHT:
                            chassis.followTrajectoryAsync(parkRight);
                            break;
                    }
                }
                break;

            case PARK:

                break;

        }
    }

    public void SlidesControl(){

    }

    public void ArmControl(){

    }

    public void ClawControl(){

    }
}