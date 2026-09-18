terraform {
  required_version = ">= 1.5.0"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0.0"
    }
  }
}

provider "oci" {
  region = var.region
}

variable "region" {
  type = string
  default = "us-ashburn-1"
}

variable "compartment_ocid" {
  type = string
}

variable "availability_domain" {
  type = string
}

variable "ssh_public_key" {
  type = string
}

variable "vm_shape" {
  type = string
  default = "VM.Standard.A1.Flex"
}

variable "vm_ocpus" {
  type = number
  default = 1
}

variable "vm_memory_gb" {
  type = number
  default = 6
}

resource "oci_core_vcn" "main" {
  cidr_blocks  = ["10.0.0.0/16"]
  compartment_id = var.compartment_ocid
  display_name = "orderflow-vcn"
}

resource "oci_core_internet_gateway" "main" {
  compartment_id = var.compartment_ocid
  vcn_id = oci_core_vcn.main.id
  display_name = "orderflow-igw"
}

resource "oci_core_default_route_table" "main" {
  manage_default_resource_id = oci_core_vcn.main.default_route_table_id
  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.main.id
  }
}

resource "oci_core_security_list" "main" {
  compartment_id = var.compartment_ocid
  vcn_id = oci_core_vcn.main.id
  display_name = "orderflow-security-list"

  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 22
      max = 22
    }
  }

  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 80
      max = 80
    }
  }

  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 443
      max = 443
    }
  }
}

resource "oci_core_instance" "orderflow_vm" {
  availability_domain = var.availability_domain
  compartment_id = var.compartment_ocid
  shape = var.vm_shape

  shape_config {
    ocpus = var.vm_ocpus
    memory_in_gbs = var.vm_memory_gb
  }

  source_details {
    source_id   = "ocid1.image.oc1..aaaaaaaacda5p7p2swwh6z7d5qvtj7n3j7onr3o6kf2g2szj7b2vgn5g7d3a"
    source_type = "image"
  }

  display_name = "orderflow-vm"

  create_vnic_details {
    subnet_id = oci_core_subnet.public.id
    assign_public_ip = true
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
  }
}

resource "oci_core_subnet" "public" {
  compartment_id = var.compartment_ocid
  vcn_id = oci_core_vcn.main.id
  cidr_block = "10.0.1.0/24"
  route_table_id = oci_core_vcn.main.default_route_table_id
  security_list_ids = [oci_core_security_list.main.id]
  display_name = "orderflow-public-subnet"
}
