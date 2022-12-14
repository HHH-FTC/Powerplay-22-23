package org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.OpModes.TeleOp_OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.Mechanisms.Chassis;
import org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.Mechanisms.Claw;
import org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.Mechanisms.ServoIntakeArm;
import org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.Mechanisms.ViperSlides;
import org.firstinspires.ftc.team8109_Rise.Robots.SlidesBot.Mechanisms.Wrist;

@TeleOp
public class SlidesBot_TeleOp extends LinearOpMode {

    @Override
    public void runOpMode() throws InterruptedException {
        // Creating objects of each mechanism
        ViperSlides slides = new ViperSlides(gamepad1, telemetry, hardwareMap);
        Chassis chassis = new Chassis(gamepad1, telemetry, hardwareMap);
        Claw claw = new Claw(gamepad1, telemetry, hardwareMap);
        Wrist wrist = new Wrist(gamepad1, hardwareMap);
        ServoIntakeArm arm = new ServoIntakeArm(gamepad1, telemetry, hardwareMap);
        Servo fiveturn = hardwareMap.get(Servo.class, "fiveturn");

        telemetry.addLine("Waiting For Start");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()){
            // Setting methods from mechanism classes to be looped
            chassis.ManualDrive();
//
            slides.setSlidePower();
            slides.slidesTelemetry();
            slides.toggleStates();
//
            arm.togglePosition();
            arm.setTelemetry();
//
            claw.toggleClaw();
            claw.setPosition();
            claw.setTelemetry();
//
            wrist.setPosition();

            // Updating telemetry to display all of the telemetry in the telemetry queue on the driver station
            telemetry.update();

            //for the five turn servo odometry
            if (gamepad1.dpad_left) {
                fiveturn.setPosition(0.93);
            } else if (gamepad1.dpad_right) {
                fiveturn.setPosition(0.97);
            }
        }
    }
}
