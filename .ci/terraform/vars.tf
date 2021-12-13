variable "aws_profile" {
  description = "Name of the public subnet of az"
  default     = "default"
}

variable "region" {
  description = "Region for launching subnets"
  default     = "us-east-1"
}
variable "volumesize" {
  default = 35
}
variable "devicename" {
  default = "/dev/sda1"
}




# EC2 variables
//Run number of test case based on count value: "default = 58" means run all builds except KPP
#DAVE - If this is set to "1", then only wrf_testcase_1.sh is run
variable "instance_count" {default =58 }
variable "instance_name"               { default = "wrf-test" }
variable "instance_profile"            { default = "WRFS3Role" }

variable "ami"                         { default = "ami-0cd4a8daf73c05400" }  #fifteenthtry & fourteenthtry
# variable "ami"                         { default = "ami-023f6d664346f3e84" }  #fourteenthtry
# variable "ami"                         { default = "ami-06e599aaf4015dafd" }  #thirteenthtry & sixthtry
variable "availability_zone"           { default = ""   }
variable "ebs_optimized"               { default = false  }
variable "instance_type_1"             { default = "c5a.4xlarge" }   # DAVE - larger for chem builds
variable "instance_type"               { default = "c5a.xlarge" }    # DAVE - smaller machines
# variable "instance_type"               { default = "t3.xlarge" }
variable "key_name"                    { default = "jenkins" }
variable "monitoring"                  { default = false  }
variable "security_group_ids"          { default = ["sg-0dfbfc9d0b4b1b519"] }
variable "subnet_id"                   { default = "subnet-010ff527a8af9ab9e" }
# variable "associate_public_ip_address" { default = false  }
variable "iam_instance_profile"        { default = "" }
variable "user_data"                   { default = " "}
variable "tags"                        { default = {} }
