package model

type Admin struct {
	Key    string  `bson:"_id,omitempty" json:"key"`
	Groups []int64 `bson:"groups" json:"groups"`
}

func NewAdmin() *Admin {
	return &Admin{
		Key:    "admin",
		Groups: make([]int64, 0),
	}
}

func (a *Admin) AddGroup(groupID int64) bool {
	for _, g := range a.Groups {
		if g == groupID {
			return false // Already exists
		}
	}
	a.Groups = append(a.Groups, groupID)
	return true
}

func (a *Admin) RemoveGroup(groupID int64) bool {
	for i, g := range a.Groups {
		if g == groupID {
			a.Groups = append(a.Groups[:i], a.Groups[i+1:]...)
			return true
		}
	}
	return false
}

func (a *Admin) HasGroup(groupID int64) bool {
	for _, g := range a.Groups {
		if g == groupID {
			return true
		}
	}
	return false
}
