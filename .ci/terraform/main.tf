provider "aws" {
   region     = "${var.region}"
   profile    = "${var.aws_profile}" #.aws/credentials
   }

variable "hostnames" {
   default = ["wrf_testcase_s_1.sh", "wrf_testcase_o_1.sh", "wrf_testcase_m_1.sh", "wrf_testcase_m_2.sh", "wrf_testcase_s_3.sh", "wrf_testcase_m_3.sh","wrf_testcase_s_4.sh","wrf_testcase_o_4.sh","wrf_testcase_m_4.sh","wrf_testcase_s_5.sh","wrf_testcase_o_5.sh","wrf_testcase_m_5.sh","wrf_testcase_s_6.sh","wrf_testcase_o_6.sh","wrf_testcase_m_6.sh","wrf_testcase_s_7.sh","wrf_testcase_o_7.sh","wrf_testcase_m_7.sh","wrf_testcase_m_8.sh", "wrf_testcase_s_9.sh","wrf_testcase_o_9.sh","wrf_testcase_m_9.sh","wrf_testcase_s_10.sh","wrf_testcase_s_11.sh","wrf_testcase_o_11.sh","wrf_testcase_m_11.sh","wrf_testcase_s_12.sh","wrf_testcase_o_12.sh","wrf_testcase_m_12.sh","wrf_testcase_s_13.sh","wrf_testcase_o_13.sh","wrf_testcase_m_13.sh","wrf_testcase_s_14.sh","wrf_testcase_o_14.sh","wrf_testcase_m_14.sh","wrf_testcase_s_15.sh","wrf_testcase_o_15.sh","wrf_testcase_m_15.sh","wrf_testcase_s_16.sh","wrf_testcase_o_16.sh","wrf_testcase_m_16.sh","wrf_testcase_s_17.sh","wrf_testcase_o_17.sh","wrf_testcase_m_17.sh","wrf_testcase_s_18.sh","wrf_testcase_m_18.sh","wrf_testcase_s_19.sh","wrf_testcase_m_19.sh"]
}

# variable "hostnames" {
#    default = ["wrf_testcase_1.sh", "wrf_testcase_2.sh", "wrf_testcase_3.sh","wrf_testcase_4.sh","wrf_testcase_5.sh","wrf_testcase_6.sh","wrf_testcase_7.sh", "wrf_testcase_8.sh","wrf_testcase_9.sh","wrf_testcase_10.sh","wrf_testcase_11.sh","wrf_testcase_12.sh","wrf_testcase_13.sh","wrf_testcase_14.sh","wrf_testcase_15.sh","wrf_testcase_16.sh","wrf_testcase_17.sh","wrf_testcase_18.sh","wrf_testcase_19.sh"]
# }

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
#   instance_type                 = "${count.index > 3 && count.index <= 9 ? var.instance_type : var.instance_type_1}"
  key_name                      = "${var.key_name}"
  monitoring                    = "${var.monitoring}"
  vpc_security_group_ids        = ["${var.security_group_ids}"]
  subnet_id                     = "${var.subnet_id}"
  associate_public_ip_address   = "${var.associate_public_ip_address}"
  user_data                     = "${element(data.template_file.user-data.*.rendered, count.index)}"
  tags                          = "${merge(var.tags, map("Name", format("%s", var.instance_name)))}"

}
