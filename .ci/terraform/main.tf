provider "aws" {
   region     = "${var.region}"
   profile    = "${var.aws_profile}" #.aws/credentials
   }

# variable "hostnames" {
#    default = ["wrf_testcase_1.sh", "wrf_testcase_2.sh", "wrf_testcase_3.sh","wrf_testcase_4.sh","wrf_testcase_5.sh","wrf_testcase_6.sh","wrf_testcase_7.sh", "wrf_testcase_8.sh","wrf_testcase_9.sh","wrf_testcase_10.sh","wrf_testcase_11.sh","wrf_testcase_12.sh","wrf_testcase_13.sh","wrf_testcase_14.sh","wrf_testcase_15.sh","wrf_testcase_16.sh","wrf_testcase_17.sh","wrf_testcase_18.sh","wrf_testcase_19.sh"]
# }
# variable "hostnames" {
#    default = ["wrf_testcase_s_1.sh", "wrf_testcase_s_3.sh","wrf_testcase_s_4.sh","wrf_testcase_s_5.sh","wrf_testcase_s_6.sh","wrf_testcase_s_7.sh", "wrf_testcase_s_9.sh","wrf_testcase_s_10.sh","wrf_testcase_s_11.sh","wrf_testcase_s_12.sh","wrf_testcase_s_13.sh","wrf_testcase_s_14.sh","wrf_testcase_s_15.sh","wrf_testcase_s_16.sh","wrf_testcase_s_17.sh","wrf_testcase_s_18.sh","wrf_testcase_s_19.sh"]
# }
variable "hostnames" {
   default = ["wrf_testcase_20.sh","wrf_testcase_21.sh","wrf_testcase_22.sh","wrf_testcase_23.sh","wrf_testcase_24.sh","wrf_testcase_25.sh","wrf_testcase_26.sh","wrf_testcase_27.sh","wrf_testcase_28.sh","wrf_testcase_29.sh","wrf_testcase_30.sh","wrf_testcase_31.sh","wrf_testcase_32.sh","wrf_testcase_33.sh","wrf_testcase_34.sh","wrf_testcase_35.sh","wrf_testcase_36.sh","wrf_testcase_37.sh","wrf_testcase_38.sh","wrf_testcase_39.sh","wrf_testcase_40.sh","wrf_testcase_41.sh","wrf_testcase_42.sh","wrf_testcase_43.sh","wrf_testcase_44.sh","wrf_testcase_45.sh","wrf_testcase_46.sh","wrf_testcase_47.sh","wrf_testcase_48.sh","wrf_testcase_49.sh","wrf_testcase_50.sh","wrf_testcase_51.sh","wrf_testcase_52.sh","wrf_testcase_53.sh","wrf_testcase_54.sh","wrf_testcase_55.sh","wrf_testcase_56.sh","wrf_testcase_57.sh","wrf_testcase_58.sh","wrf_testcase_59.sh","wrf_testcase_60.sh","wrf_testcase_61.sh","wrf_testcase_62.sh","wrf_testcase_63.sh","wrf_testcase_64.sh","wrf_testcase_65.sh","wrf_testcase_66.sh","wrf_testcase_67.sh"]
}

data "template_file" "user-data" {
     count = "${length(var.hostnames)}"
     template = "${file("${element(var.hostnames, count.index)}")}"
}

resource "aws_instance" "application" {
  count                         = "${var.instance_count}"
  ami                           = "${var.ami}"
  iam_instance_profile          = "${var.instance_profile}"
  ebs_block_device              = {
     device_name = "${var.devicename}" 
     volume_size = "${var.volumesize}"
                                  }
  availability_zone             = "${var.availability_zone}"
  ebs_optimized                 = "${var.ebs_optimized}"
  instance_type                 = "${var.instance_type}"
#   instance_type                 = "${count.index >= 3 && count.index <= 5 && count.index && count.index >= 36 && count.index <= 41 ? var.instance_type_1 : var.instance_type}"
  key_name                      = "${var.key_name}"
  monitoring                    = "${var.monitoring}"
  vpc_security_group_ids        = ["${var.security_group_ids}"]
  subnet_id                     = "${var.subnet_id}"
  associate_public_ip_address   = "${var.associate_public_ip_address}"
  user_data                     = "${element(data.template_file.user-data.*.rendered, count.index)}"
  tags                          = "${merge(var.tags, map("Name", format("%s", var.instance_name)))}"

}
