import { useParams } from 'react-router-dom'
import FollowList from '../../components/FollowList'

export default function FollowingsPage() {
  const { id } = useParams<{ id: string }>()
  return <FollowList userId={Number(id)} mode="followings" />
}
